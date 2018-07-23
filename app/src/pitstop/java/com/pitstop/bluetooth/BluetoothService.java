package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator;
import com.pitstop.bluetooth.communicator.ObdManager;
import com.pitstop.bluetooth.dataPackages.CastelPidPackage;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;
import com.pitstop.bluetooth.handler.AlarmHandler;
import com.pitstop.bluetooth.handler.BluetoothDataHandlerManager;
import com.pitstop.bluetooth.handler.DtcDataHandler;
import com.pitstop.bluetooth.handler.FreezeFrameDataHandler;
import com.pitstop.bluetooth.handler.FuelHandler;
import com.pitstop.bluetooth.handler.PidDataHandler;
import com.pitstop.bluetooth.handler.VinDataHandler;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.other.DeviceClockSyncUseCase;
import com.pitstop.models.Alarm;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.AlarmObservable;
import com.pitstop.observer.AlarmObserver;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.BluetoothDtcObserver;
import com.pitstop.observer.BluetoothPidObserver;
import com.pitstop.observer.BluetoothRtcObserver;
import com.pitstop.observer.BluetoothVinObserver;
import com.pitstop.observer.ConnectionStatusObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.observer.DeviceVerificationObserver;
import com.pitstop.observer.FuelObservable;
import com.pitstop.observer.FuelObserver;
import com.pitstop.observer.Observer;
import com.pitstop.repositories.Repository;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NotificationsHelper;
import com.pitstop.utils.TimeoutTimer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothService extends Service implements ObdManager.IBluetoothDataListener
        , BluetoothConnectionObservable, ConnectionStatusObserver, BluetoothDataHandlerManager
        , DeviceVerificationObserver, BluetoothWriter, AlarmObservable, FuelObservable {

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private boolean overWriteInterval = false;
    private int debugDrawerInterval = 4;
    private String ElmMacAddress ="";

    public static final int notifID = 1360119;
    private static final String TAG = BluetoothService.class.getSimpleName();

    //Timer length values
    public static final int DTC_RETRY_LEN = 3; //Seconds
    public static final int DTC_RETRY_COUNT = 4;
    public static final int PID_RETRY_LEN = 5; //Seconds
    public static final int PID_RETRY_COUNT = 2;
    private final int RTC_RETRY_LEN = 5; //Seconds
    private final int RTC_RETRY_COUNT = 0;
    private final int VERIFICATION_TIMEOUT = 15; //Seconds
    private final int PERIOD_TRACK_PID_LEN = 60; //Seconds
    private final int PERIOD_RESET_DEV_TM = 1800; //Seconds
    private final int PERIOD_RTC_LEN = 60000; //Milliseconds
    private final int PERIOD_VIN_LEN = 10000; //Milliseconds

    //Flags
    private boolean vinRequested = false;
    private boolean deviceIsVerified = false;
    private boolean ignoreVerification = false; //Whether to begin verifying device by VIN or not
    private boolean rtcTimeRequested = false;
    private boolean allowPidTracking = true;
    private boolean allPidRequested = false;
    private boolean dtcRequested = false;
    private boolean receivedDtcResponse = false;
    private boolean readyForDeviceTime = true;

    //Connection state values
    private long terminalRtcTime = -1;
    private String currentDeviceId = "";
    private String deviceConnState = State.DISCONNECTED;


    //Data is passed down to these fellas so they can deal with it
    private PidDataHandler pidDataHandler;
    private DtcDataHandler dtcDataHandler;
    private VinDataHandler vinDataHandler;
    private FreezeFrameDataHandler freezeFrameDataHandler;

    //Other useful objects
    private final IBinder mBinder = new BluetoothBinder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler backgroundHandler = new Handler();
    private final BluetoothServiceBroadcastReceiver connectionReceiver
            = new BluetoothServiceBroadcastReceiver(this);

    private UseCaseComponent useCaseComponent;
    private ReadyDevice readyDevice;
    private BluetoothDeviceManager deviceManager;
    private DtcPackage requestedDtcs;
    private List<Observer> observerList = Collections.synchronizedList(new ArrayList<>());
    private AlarmHandler alarmHandler;
    private FuelHandler fuelHandler;

    /**Resetting flag that allows for the execution of the device clock sync use case
     * this is done so that the use case is only executed once every 600 seconds**/
    private final TimeoutTimer readyForDeviceTimeResetTimer
            = new TimeoutTimer(PERIOD_RESET_DEV_TM,0) {
        @Override
        public void onRetry() {}

        @Override
        public void onTimeout() {
            Log.d(TAG,"readyForDeviceTimeResetTimer.onTimeout()");
            readyForDeviceTime = true;
        }
    };

    /**For tracking pid in mixpanel helper**/
    private final TimeoutTimer pidTrackTimeoutTimer
            = new TimeoutTimer(PERIOD_TRACK_PID_LEN,0) {
        @Override
        public void onRetry() {}

        @Override
        public void onTimeout() {
            Log.d(TAG,"pidTrackTimeoutTimer.onTimeout()");
            allowPidTracking = true;
        }
    };

    /**For when VIN isn't returned from device(usually means ignition isn't ON)**/
    private final TimeoutTimer getVinTimeoutTimer
            = new TimeoutTimer(VERIFICATION_TIMEOUT,0) {
        @Override
        public void onRetry() {}

        @Override
        public void onTimeout() {
            Log.d(TAG,"getVinTimeoutTimer() timeout, vinRequested: "+vinRequested);
            if (!vinRequested) return;
            vinRequested = false;
            Logger.getInstance().logW(TAG,"VIN retrieval timeout", DebugMessage.TYPE_BLUETOOTH);

            //For verification progress
            if (deviceConnState.equals(State.CONNECTED_UNVERIFIED)){
                if (deviceManager.moreDevicesLeft()){

                    setConnectionState(State.SEARCHING);
                    notifySearchingForDevice();
                }
                else{
                    setConnectionState(State.DISCONNECTED);
                    notifyDeviceDisconnected();
                }
                onConnectedDeviceInvalid();
            }
        }
    };

    /**Request DTC Data **/
    private final TimeoutTimer dtcTimeoutTimer
            = new TimeoutTimer(DTC_RETRY_LEN,DTC_RETRY_COUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"dtcTimeoutTimer.onRetry() dtcRequested? "+dtcRequested);
            if (!dtcRequested) return;
            deviceManager.getDtcs();
        }

        @Override
        public void onTimeout() {
            if (!dtcRequested) return;
            Logger.getInstance().logW(TAG,"DTC retrieval timeout", DebugMessage.TYPE_BLUETOOTH);

            if (!receivedDtcResponse) notifyErrorGettingDtcData();
            else notifyDtcData(requestedDtcs);
        }
    };

    /**Request All PID Data **/
    private final TimeoutTimer pidTimeoutTimer
            = new TimeoutTimer(PID_RETRY_LEN,PID_RETRY_COUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"pidTimeoutTimer.onRetry() allPidRequested? "+allPidRequested);
            requestAllPid();
        }

        @Override
        public void onTimeout() {
            //Pid data wasn't sent before timer is done
            if (allPidRequested){
                notifyErrorGettingAllPid();
                Logger.getInstance().logW(TAG,"Get all pids retrieval timeout", DebugMessage.TYPE_BLUETOOTH);

            }
        }
    };

    /**Request Device Time **/
    private final TimeoutTimer rtcTimeoutTimer
            = new TimeoutTimer(RTC_RETRY_LEN,RTC_RETRY_COUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"rtcTimeoutTimer.onRetry() rtcTimeRequested? "+rtcTimeRequested);
        }

        @Override
        public void onTimeout() {
            //Rtc data wasn't sent before timer is done
            if (rtcTimeRequested){
                notifyErrorGettingRtcTime();
                Logger.getInstance().logW(TAG,"Rtc time retrieval timeout", DebugMessage.TYPE_BLUETOOTH);

            }
        }
    };

    /**Sometimes terminal time might not be returned**/
    private final Runnable periodicGetTerminalTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (deviceConnState.equals(State.CONNECTED_VERIFIED)){
                Log.d(TAG,"Periodic get terminal time request executing");
                requestDeviceTime();
            }
            backgroundHandler.postDelayed(this, PERIOD_RTC_LEN);
        }
    };

    /**Sometimes vin might not be returned**/
    private final Runnable periodicGetVinRunnable = new Runnable() {
        @Override
        public void run() {
            boolean requestVin = deviceConnState.equals(State.CONNECTED_UNVERIFIED)
                    || ignoreVerification;
            if (requestVin){
                Log.d(TAG,"Periodic VIN request.");
                deviceManager.getVin();
            }
            backgroundHandler.postDelayed(this, PERIOD_VIN_LEN);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getBaseContext()))
                .build();

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if(deviceManager != null) {
                deviceManager.close();
                deviceManager = null;
            }
            deviceManager = new BluetoothDeviceManager(this);
            deviceManager.setBluetoothDataListener(this);
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);

        this.pidDataHandler = new PidDataHandler(this,getBaseContext());
        this.dtcDataHandler = new DtcDataHandler(this,useCaseComponent);
        this.vinDataHandler = new VinDataHandler(this,this,this);
        this.freezeFrameDataHandler = new FreezeFrameDataHandler(this,getBaseContext());
        this.alarmHandler = new AlarmHandler(this, useCaseComponent);
        this.fuelHandler = new FuelHandler(this, useCaseComponent);
        backgroundHandler.postDelayed(periodicGetTerminalTimeRunnable, 10000);
        backgroundHandler.postDelayed(periodicGetVinRunnable,5000);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Running on start command - auto-connect service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroying auto-connect service");

        try {
            unregisterReceiver(connectionReceiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
        if (deviceManager != null)
            deviceManager.close();
        deviceManager = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDevicesFound() {
        Log.d(TAG, "onDevicesFound()");
        if (deviceConnState.equals(State.SEARCHING)){
            setConnectionState(State.FOUND_DEVICES);
            notifyFoundDevices();
        }
    }

    @Override
    public void getBluetoothState(int state) {

        switch(state){
            case IBluetoothCommunicator.CONNECTING:
                Log.d(TAG,"getBluetoothState() state: connecting");

                if (deviceConnState.equals(State.SEARCHING)
                        || deviceConnState.equals(State.DISCONNECTED)
                        || deviceConnState.equals(State.FOUND_DEVICES)){
                    setConnectionState(State.CONNECTING);
                    notifyDeviceConnecting();
                }

                break;
            case IBluetoothCommunicator.CONNECTED:
                Log.d(TAG,"getBluetoothState() state: "+deviceConnState);

                /*Check to make sure were not overriding the state once
                ** its already verified and connected */
                if (!deviceConnState.equals(State.CONNECTED_UNVERIFIED)
                        && !deviceConnState.equals(State.CONNECTED_VERIFIED)
                        && !deviceConnState.equals(State.VERIFYING)){
                    clearInvalidDeviceData();
                    resetConnectionVars();
                    if (deviceManager.getDeviceType() == BluetoothDeviceManager.DeviceType.OBD212) //Sync time for 212 devices
                        deviceManager.setRtc(System.currentTimeMillis());
                    requestVin();                //Get VIN to validate car
                    setConnectionState(State.CONNECTED_UNVERIFIED);
                    notifyVerifyingDevice();     //Verification in progress
                    requestDeviceTime();          //Get RTC and mileage once connected
                    deviceManager.requestData(); //Request data upon connecting

                }

                break;
            case IBluetoothCommunicator.DISCONNECTED:
                Log.d(TAG,"getBluetoothState() state: disconnected");
                //Only notify that device disonnected if a verified connection was established previously
                if ((deviceIsVerified && !ignoreVerification) || !deviceManager.moreDevicesLeft()){
                    setConnectionState(State.DISCONNECTED);
                    notifyDeviceDisconnected();
                    resetConnectionVars();
                    NotificationsHelper.cancelConnectedNotification(getBaseContext());
                }

                break;
        }
    }

    private void notifyFoundDevices() {
        Log.d(TAG, "notifyFoundDevices()");
        Logger.getInstance().logI(TAG,"Devices Found", DebugMessage.TYPE_BLUETOOTH);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onFoundDevices());
            }
        }

    }

    private void notifyDeviceConnecting() {
        Log.d(TAG, "notifyDeviceConnecting()");
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onConnectingToDevice());
            }
        }

    }

    @Override
    public void subscribe(Observer observer) {
        Log.d(TAG,"subscribe()");
        if (!observerList.contains(observer)){
            observerList.add(observer);
        }
    }

    @Override
    public void unsubscribe(Observer observer) {
        Log.d(TAG,"unsubscribe()");
        if (observerList.contains(observer)){
            observerList.remove(observer);
        }
    }

    @Override
    public void onHandlerReadVin(String vin) {
        Log.d(TAG,"onHandlersReadVin() vin: "+vin);
        notifyVin(vin);
    }

    @Override
    public long getRtcTime() {
        Log.d(TAG,"getBluetoothDeviceTime()");
        return terminalRtcTime;
    }

    @Override
    public void onHandlerVerifyingDevice() {
        Log.d(TAG,"onHandlerVerifyingDevice()");
        setConnectionState(State.VERIFYING);
        notifyVerifyingDevice();
    }

    @Override
    public String getDeviceState() {
        Log.d(TAG,"getDeviceState()");
        return deviceConnState;
    }

    @Override
    public ReadyDevice getReadyDevice() {
        Log.d(TAG,"getReadyDevie()");
        if (deviceConnState.equals(State.CONNECTED_VERIFIED)){
            return readyDevice;
        }
        return null;
    }

    @Override
    public void requestDeviceSync() {
        Log.d(TAG,"requestDeviceSync()");
        deviceManager.setRtc(System.currentTimeMillis());
        notifySyncingDevice();
    }

    @Override
    public void setPidsToBeSent(String pids, int timeInterval) {
        Log.d(TAG,"setPidsToBeSent() pids: "+pids+", timeInterval: "+timeInterval);
        deviceManager.setPidsToSend(pids, timeInterval);
    }

    @Override
    public boolean requestPidInitialization() {
        if (deviceConnState.equals(State.CONNECTED_VERIFIED) && readyDevice != null
                && readyDevice.getVin() != null){
            Log.d(TAG,"requestPidInitialization() readyDevice.getVin(): "+readyDevice.getVin());
            pidDataHandler.setDefaultPidCommunicationParameters(readyDevice.getVin());
            return true;
        }
        return false;
    }

    @Override
    public boolean requestDtcData() {
        if (dtcRequested) return false;
        Logger.getInstance().logI(TAG,"Engine codes requested", DebugMessage.TYPE_BLUETOOTH);

        dtcRequested = true;
        requestedDtcs = null;
        dtcTimeoutTimer.cancel();
        dtcTimeoutTimer.startTimer();
        deviceManager.getDtcs();
        return true;
    }

    @Override
    public boolean requestVin() {
        if (vinRequested) return false;
        Logger.getInstance().logI(TAG,"VIN requested", DebugMessage.TYPE_BLUETOOTH);
        vinRequested = true;

        getVinTimeoutTimer.cancel();
        getVinTimeoutTimer.startTimer();

        deviceManager.getVin();
        return true;
    }

    @Override
    public boolean requestAllPid() {
        if (allPidRequested) return false;
        Logger.getInstance().logI(TAG,"All pid requested", DebugMessage.TYPE_BLUETOOTH);

        allPidRequested = true;
        pidTimeoutTimer.cancel();
        pidTimeoutTimer.startTimer();
        deviceManager.requestSnapshot();
        return true;
    }

    @Override
    public boolean requestDeviceTime() {
        if (rtcTimeRequested) return false;
        rtcTimeRequested = true;
        Logger.getInstance().logI(TAG,"Rtc time requested", DebugMessage.TYPE_BLUETOOTH);

        rtcTimeoutTimer.cancel();
        rtcTimeoutTimer.startTimer();
        deviceManager.getRtc();
        return true;
    }


    @Override
    public boolean requestDeviceSearch(boolean urgent, boolean ignoreVerification) {
        Log.d(TAG, "requestDeviceSearch() urgent : " + Boolean.toString(urgent) + " ignoreVerification: " + Boolean.toString(ignoreVerification));
        if (deviceManager == null) return false;
        this.ignoreVerification = ignoreVerification;
        if (urgent) deviceManager.changeScanUrgency(urgent); //Only set to more urgent to avoid automatic scans from overriding user
        vinDataHandler.vinVerificationFlagChange(ignoreVerification);
        if (!deviceConnState.equals(State.DISCONNECTED)
                && !deviceConnState.equals(State.SEARCHING)){
            Log.d(TAG, "device state is not searching or disconnected");
            Log.d(TAG, "state is : " + deviceConnState);
            return false;
        }
        Logger.getInstance().logI(TAG,"Request device search, verification ignored? "+ignoreVerification+", urgent? "+urgent
                , DebugMessage.TYPE_BLUETOOTH);

        if (deviceManager != null && deviceManager.startScan(urgent,ignoreVerification)){
            Log.d(TAG,"Started scan");
            setConnectionState(State.SEARCHING);
            notifySearchingForDevice();
        }
        else{
            Log.d(TAG,"Scan failed");
            return false;
        }
        return true;
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
    }

    /**
     * @param responsePackageInfo The response from device for a parameter that
     *                            was successfully set.
     *                            If device time was set, save the id of the device.
     */
    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

        if(responsePackageInfo.result == 1) {
            // Once device time is reset, store deviceId
            if (responsePackageInfo.deviceId != null && !responsePackageInfo.deviceId.isEmpty()){
                if (responsePackageInfo.deviceId != null
                        && !responsePackageInfo.deviceId.isEmpty()){
                    currentDeviceId = responsePackageInfo.deviceId;
                }
            }

        }
        Log.i(TAG, "Setting parameter response on service callbacks - auto-connect service");
    }

    /**
     * Handles the data returned from a parameter query command
     * @param parameterPackage
     */
    @Override
    public void parameterData(ParameterPackage parameterPackage) {
        Log.d(TAG, "parameter value: " + parameterPackage.value);
        if (parameterPackage == null) return;
        if (parameterPackage.paramType == null) return;
        if (parameterPackage.value == null) parameterPackage.value = "";

        if (parameterPackage.deviceId != null && !parameterPackage.deviceId.isEmpty()){
            currentDeviceId = parameterPackage.deviceId;
        }

        if (parameterPackage.paramType.equals(ParameterPackage.ParamType.RTC_TIME)){
            try{
                long rtcTime = Long.valueOf(parameterPackage.value);
                Logger.getInstance().logI(TAG, "Rtc time received: " + parameterPackage.value
                        , DebugMessage.TYPE_BLUETOOTH);
                if (terminalRtcTime == -1) terminalRtcTime = rtcTime;
                notifyRtc(rtcTime);

                if (readyDevice != null && readyForDeviceTime){
                    Log.d(TAG,"executing deviceClockSyncUseCase()");
                    useCaseComponent.getDeviceClockSyncUseCase().execute(rtcTime, readyDevice.getScannerId()
                            , readyDevice.getVin(), new DeviceClockSyncUseCase.Callback() {
                        @Override
                        public void onClockSynced() {
                            Log.d(TAG,"getDeviceClockSyncUseCase().onClockSynced()");
                            readyForDeviceTime = false;
                            readyForDeviceTimeResetTimer.start();
                        }

                        @Override
                        public void onError(@NotNull RequestError error) {
                            Log.d(TAG,"getDeviceClockSyncUseCase().onError() error: "+error);
                            readyForDeviceTime = false;
                            readyForDeviceTimeResetTimer.start();
                        }
                    });
                }

            }
            catch(Exception e){
                e.printStackTrace();
            }

        }
        else if (parameterPackage.paramType.equals(ParameterPackage.ParamType.VIN)){
            Logger.getInstance().logI(TAG, "Vin retrieval result: " + parameterPackage.value
                    , DebugMessage.TYPE_BLUETOOTH);
            vinDataHandler.handleVinData(parameterPackage.value
                    ,currentDeviceId,ignoreVerification);
        }
        else if (parameterPackage.paramType.equals(ParameterPackage.ParamType.SUPPORTED_PIDS)
                && readyDevice != null){
            Logger.getInstance().logI(TAG,"Got supported pid: " + parameterPackage.value
                    , DebugMessage.TYPE_BLUETOOTH);
            if (overWriteInterval){
                pidDataHandler.setDeviceRtcInterval(parameterPackage.value.split(",")
                        ,readyDevice.getVin(), debugDrawerInterval);
                overWriteInterval = false;
            }
            else {
                notifyGotSupportedPids(parameterPackage.value);
                pidDataHandler.setPidCommunicationParameters(parameterPackage.value.split(",")
                        , readyDevice.getVin());
            }
        }
    }
    public void handleVinData(String Vin, String deviceID){
        Logger.getInstance().logI(TAG, "Vin retrieval result: " + Vin
                , DebugMessage.TYPE_BLUETOOTH);
        vinDataHandler.handleVinData(Vin
                ,deviceID,ignoreVerification);
    }

    private void notifyGotSupportedPids(String value) {
        Log.d(TAG, "notifyGotSUpportedPIDs()");
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onGotSuportedPIDs(value));
            }
        }
    }


    @Override
    public void idrPidData(PidPackage pidPackage) {
        Logger.getInstance().logD(TAG, "IDR pid data received: " + (pidPackage == null ? "null" : pidPackage.toString())
                , DebugMessage.TYPE_BLUETOOTH);

        deviceManager.requestData();
        trackIdrPidData(pidPackage);

        //Set device id if its found in pid package
        if (pidPackage != null && pidPackage.getDeviceId() != null && !pidPackage.getDeviceId().isEmpty()){
            Log.d(TAG, "setting current device ID to " + pidPackage.getDeviceId());
            currentDeviceId = pidPackage.getDeviceId();
        }
        pidPackage.setDeviceId(currentDeviceId);
        pidDataHandler.handlePidData(pidPackage, vinDataHandler.getRecentVin());

        //212 pid "snapshot" broadcast logic
        if (pidPackage != null && deviceManager.getDeviceType() == BluetoothDeviceManager.DeviceType.OBD212){
            Log.d(TAG ,"deviceManager is not connected to 215");
            if (pidPackage.getPids() == null){
                pidPackage.setPids(new HashMap<>());
                notifyGotAllPid(pidPackage);
            }else{
                notifyGotAllPid(pidPackage);
            }
        }
    }

    @Override
    public void pidData(PidPackage pidPackage) {
        if (pidPackage == null)return;

        pidTimeoutTimer.cancel();
        Logger.getInstance().logI(TAG, "All pid data received: " + pidPackage.toString()
                , DebugMessage.TYPE_BLUETOOTH);
        notifyGotAllPid(pidPackage);
    }

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        if (dtcPackage == null) return;
        Logger.getInstance().logI(TAG, "Engine codes received: " + dtcPackage.toString()
                , DebugMessage.TYPE_BLUETOOTH);
        //Set device id if its found in dtc package
        if (dtcPackage.deviceId != null && !dtcPackage.deviceId.isEmpty()){
            currentDeviceId = dtcPackage.deviceId;
        }

        dtcPackage.deviceId = currentDeviceId;
        dtcDataHandler.handleDtcData(dtcPackage);
        Log.d(TAG,"requestedDtcs before appending: "+requestedDtcs);
        if (dtcRequested){
            Log.d(TAG, "setting received DTCs to true");
            appendDtc(dtcPackage);
            receivedDtcResponse = true;
        }
        Log.d(TAG,"requestedDtcs after appending: "+requestedDtcs);

    }

    private void appendDtc(DtcPackage dtcPackage){
        Log.d(TAG,"appendDtc() dtcPackage: "+dtcPackage);
        if (dtcPackage == null) return;

        if (requestedDtcs == null){
            requestedDtcs = new DtcPackage();
            requestedDtcs.rtcTime = dtcPackage.rtcTime;
            requestedDtcs.deviceId = dtcPackage.deviceId;
            requestedDtcs.dtcs = new HashMap<>();
        }

        for (Map.Entry<String,Boolean> dtc: dtcPackage.dtcs.entrySet()){
            //Check whether requested dtcs already contains same <String,Boolean> pair, if not add
            if (!requestedDtcs.dtcs.containsKey(dtc.getKey())
                    && !(requestedDtcs.dtcs.containsKey(dtc.getKey())
                            && requestedDtcs.dtcs.get(dtc.getKey()).equals(dtc.getValue()))){

                Log.d(TAG,"appendDtc() <String,Value>: "+dtc.getKey()+", "+dtc.getValue());
                requestedDtcs.dtcs.put(dtc.getKey(),dtc.getValue());
            }
        }
    }

    @Override
    public void setDeviceName(String deviceName) {
        Log.d(TAG, "setDeviceName: " +deviceName);
        this.ElmMacAddress =deviceName;
        currentDeviceId = deviceName;
    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {
        Log.d(TAG,"ffData()");
        //Set device id if its found in freeze frame package
        if (ffPackage.deviceId != null && !ffPackage.deviceId.isEmpty()){
            currentDeviceId = ffPackage.deviceId;
        }

        ffPackage.deviceId = currentDeviceId;
        freezeFrameDataHandler.handleFreezeFrameData(ffPackage);
    }

    @Override
    public void scanFinished() {

        Log.d(TAG, "scanFinished(), deviceConnState: " + deviceConnState
                + ", deviceManager.moreDevicesLeft?" + deviceManager.moreDevicesLeft());
        if (deviceConnState.equals(State.SEARCHING) || deviceConnState.equals(State.FOUND_DEVICES)){
            setConnectionState(State.DISCONNECTED);
            notifyDeviceDisconnected();
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            // Here's where the app get the device id for the first time

            Logger.getInstance().logD(TAG, "Login package: " + loginPackageInfo.toString()
                    , DebugMessage.TYPE_BLUETOOTH);

            if (loginPackageInfo.deviceId != null && !loginPackageInfo.deviceId.isEmpty()){
                currentDeviceId = loginPackageInfo.deviceId;
            }

            deviceManager.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);

        } else if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }
    }

    @Override
    public void onVerificationSuccess(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification success",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }

        deviceIsVerified = true;
        setConnectionState(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
        readyDevice = new ReadyDevice(vin, currentDeviceId, currentDeviceId);
        notifyDeviceReady(vin,currentDeviceId,currentDeviceId);
        deviceManager.getSupportedPids();

    }

    @Override
    public void onVerificationDeviceBrokenAndCarMissingScanner(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to broken device, car has no scanner",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            onVerificationSuccess(vin);
            return;
        }

        MainActivity.Companion.setAllowDeviceOverwrite(true);
        deviceIsVerified = true;
        setConnectionState(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
        deviceManager.onConnectDeviceValid();
        notifyDeviceNeedsOverwrite();
        readyDevice = new ReadyDevice(vin,currentDeviceId,currentDeviceId);
        notifyDeviceReady(vin,currentDeviceId,currentDeviceId);
        sendConnectedNotification();
        deviceManager.getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceBrokenAndCarHasScanner(String vin, String deviceId) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to broken device, car has scanner",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            onVerificationSuccess(vin);
            return;
        }

        currentDeviceId = deviceId; //Set it to the device id found on back-end
        setDeviceNameAndId(deviceId);
        deviceIsVerified = true;
        setConnectionState(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
        deviceManager.onConnectDeviceValid();
        readyDevice = new ReadyDevice(vin,deviceId,deviceId);
        notifyDeviceReady(vin,deviceId,deviceId);
        sendConnectedNotification();
        deviceManager.getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceInvalid(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to invalid device",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            onVerificationSuccess(vin);
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;
        onConnectedDeviceInvalid();

    }

    @Override
    public void onVerificationDeviceAlreadyActive(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to device being already active",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }else if (ignoreVerification){
            onVerificationSuccess(vin);
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;
        onConnectedDeviceInvalid();

    }

    @Override
    public void onVerificationError(String vin) {

        Logger.getInstance().logI(TAG,"VIN verification failed due to error",
                DebugMessage.TYPE_BLUETOOTH);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
            return;
        }else if (ignoreVerification){
            onVerificationSuccess(vin);
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;
        onConnectedDeviceInvalid();
    }

    @Override
    public void onBluetoothOn() {
        Logger.getInstance().logI(TAG,"Bluetooth OFF",
                DebugMessage.TYPE_BLUETOOTH);
        if (deviceManager != null) {
            deviceManager.close();
        }

        deviceManager = new BluetoothDeviceManager(this);

        deviceManager.setBluetoothDataListener(this);
        if (BluetoothAdapter.getDefaultAdapter()!=null
                && BluetoothAdapter.getDefaultAdapter().isEnabled()) {

            requestDeviceSearch(true,false); // start search when turning bluetooth on
        }
    }

    @Override
    public void onBluetoothOff() {
        Logger.getInstance().logI(TAG,"Bluetooth OFF",
                DebugMessage.TYPE_BLUETOOTH);
        Log.d(TAG,"onBluetoothOff()");
        setConnectionState(State.DISCONNECTED);
        notifyDeviceDisconnected();
        currentDeviceId = null;
        deviceManager.bluetoothStateChanged(BluetoothAdapter.STATE_OFF); //CONTINUE HERE
        deviceManager.close();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(BluetoothService.notifID);
    }

    @Override
    public void onConnectedToInternet(){
        Log.i(TAG, "Sending stored PIDS and DTCS");
        dtcDataHandler.sendLocalDtc();
    }

    @Override
    public void requestFreezeData() {
        Log.d(TAG,"requestFreezeData()");
        deviceManager.getFreezeFrame();
    }

    @Override
    public boolean isDeviceVerified() {
        Log.d(TAG,"isDeviceVerified()");
        return deviceIsVerified;
    }

    @Override
    public BluetoothDeviceManager.DeviceType getDeviceType(){
        Log.d(TAG,"getDeviceType");
        if (deviceManager == null) return null;
        return deviceManager.getDeviceType();
    }

    public void setDeviceNameAndId(String name){
        Log.d(TAG,"setDeviceNameAndId() name: "+name);
        deviceManager.setDeviceNameAndId(name);
    }

    private void notifyDeviceNeedsOverwrite() {
        Log.d(TAG,"notifyDeviceNeedsOverwrite()");
        for (Observer o : observerList) {
            if (o instanceof Device215BreakingObserver) {
                mainHandler.post(() ->
                        ((Device215BreakingObserver) o).onDeviceNeedsOverwrite());
            }
        }
    }

    private void notifySearchingForDevice() {
        Log.d(TAG,"notifySearchingForDevice()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                mainHandler.post(()
                        -> ((BluetoothConnectionObserver)observer).onSearchingForDevice());
            }
        }
    }

    private void notifyDeviceReady(String vin, String scannerId, String scannerName) {
        Log.d(TAG,"notifyDeviceReady() vin: "+vin+", scannerId:"+scannerId
                +", scannerName: "+scannerName);
        // this is for carista testing make sure to remove this
        if (scannerName == null || scannerId == null){
            String temp = ElmMacAddress;
            for (Observer observer: observerList){
                if (observer instanceof BluetoothConnectionObserver){
                    mainHandler.post(() -> ((BluetoothConnectionObserver)observer)
                            .onDeviceReady(new ReadyDevice(vin, temp, temp)));
                }
            }

        }else {
        for (Observer observer: observerList) {
            if (observer instanceof BluetoothConnectionObserver) {
                mainHandler.post(() -> ((BluetoothConnectionObserver) observer)
                        .onDeviceReady(new ReadyDevice(vin, scannerId, scannerName)));
            }
        }
        }
    }

    private void notifyDeviceDisconnected() {
        Log.d(TAG,"notifyDeviceDisconnected()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                mainHandler.post(()
                        -> ((BluetoothConnectionObserver)observer).onDeviceDisconnected());
            }
        }
    }

    private void notifyVerifyingDevice() {
        Log.d(TAG,"notifyVerifyingDevice()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                mainHandler.post(()
                        -> ((BluetoothConnectionObserver)observer).onDeviceVerifying());
            }
        }
    }

    private void notifySyncingDevice() {
        Log.d(TAG,"notifySyncingDevice()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                mainHandler.post(()
                        -> ((BluetoothConnectionObserver)observer).onDeviceSyncing());
            }
        }
    }

    private void notifyDtcData(DtcPackage dtc) {
        Log.d(TAG,"notifyDtcData() "+dtc);
        if (!dtcRequested) return;
        dtcRequested = false;
        receivedDtcResponse = false; //Reset flag

        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                mainHandler.post(()
                        -> ((BluetoothDtcObserver) observer).onGotDtc(dtc));
            }
        }
    }

    private void notifyErrorGettingDtcData() {
        Log.d(TAG,"notifyErrorGettingDtcData()");
        if (!dtcRequested) return;
        dtcRequested = false;
        receivedDtcResponse = false;

        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                mainHandler.post(()
                        -> ((BluetoothDtcObserver) observer).onErrorGettingDtc());
            }
        }
    }

    private void notifyVin(String vin) {
        Log.d(TAG,"notifyVin() vin: "+vin);
        if (!vinRequested) return;

        vinRequested = false;
        getVinTimeoutTimer.cancel();
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothVinObserver) {
                mainHandler.post(()
                        -> ((BluetoothVinObserver)observer).onGotVin(vin));
            }
        }
    }

    @Override
    public void onGotRtc(long l) {
        notifyRtc(l);
    }

    private void notifyRtc(Long rtc) {
        Log.d(TAG,"notifyRtc() rtc: "+rtc);
        if (!rtcTimeRequested) return;

        rtcTimeRequested = false;
        rtcTimeoutTimer.cancel();
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothRtcObserver) {
                mainHandler.post(()
                        -> ((BluetoothRtcObserver)observer).onGotDeviceTime(rtc));
            }
        }
    }

    private void notifyErrorGettingRtcTime(){
        Log.d(TAG,"notifyErrorGettingRtcTime()");
        if (!rtcTimeRequested) return;
        rtcTimeRequested = false;

        for (Observer observer : observerList) {
            if (observer instanceof BluetoothRtcObserver) {
                mainHandler.post(()
                        -> ((BluetoothRtcObserver)observer).onErrorGettingDeviceTime());
            }
        }

    }

    private void notifyGotAllPid(PidPackage pidPackage){
        Log.d(TAG,"notifyGotAllPid() pidPackage: "+pidPackage);
        /*if (!allPidRequested){{
            Log.d(TAG, "allPidRequested is false so im not gonna notify got all pid");
            return;
        }}*/
        allPidRequested = false;
        pidTimeoutTimer.cancel();
        for (Observer observer: observerList){
            Log.d(TAG, "notifying that pids were received");
            if (observer instanceof BluetoothPidObserver){
                mainHandler.post(()
                        -> ((BluetoothPidObserver)observer).onGotAllPid(pidPackage));
            }
        }
    }

    private void notifyErrorGettingAllPid(){
        Log.d(TAG,"notifyErrorGettingAllPid()");
        if (!allPidRequested) return;
        allPidRequested = false;

        for (Observer observer: observerList){
            if (observer instanceof BluetoothPidObserver){
                mainHandler.post(()
                        -> ((BluetoothPidObserver)observer).onErrorGettingAllPid());
            }
        }
    }

    private void sendConnectedNotification(){

        useCaseComponent.getUserCarUseCase().execute(Repository.DATABASE_TYPE.LOCAL
                , new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                String carName = "Click here to find out more" +
                        car.getYear() + " " + car.getMake() + " " + car.getModel();
                NotificationsHelper.sendNotification(getBaseContext()
                        ,carName, "Car is Connected");
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                NotificationsHelper.sendNotification(getBaseContext()
                        ,"Click here to find out more", "Car is Connected");
            }

            @Override
            public void onError(RequestError error) {

            }
        });
    }

    //Remove data that was acquired from the wrong device during the VIN verification process
    private void clearInvalidDeviceData(){
        pidDataHandler.clearPendingData();
        dtcDataHandler.clearPendingData();
    }

    private void onConnectedDeviceInvalid(){
        Log.d(TAG,"onConnectedDeviceInvalid()");
        if (deviceManager.moreDevicesLeft() && deviceManager.connectToNextDevice()){
            setConnectionState(State.SEARCHING);
            notifySearchingForDevice();
        }else{
            deviceManager.closeDeviceConnection();
            setConnectionState(State.DISCONNECTED);
            notifyDeviceDisconnected();
        }
    }

    private void resetConnectionVars(){
        Log.d(TAG,"resetConnectionVars()");
        dtcTimeoutTimer.cancel();
        getVinTimeoutTimer.cancel();
        pidTimeoutTimer.cancel();
        rtcTimeoutTimer.cancel();
        terminalRtcTime = -1;
        allPidRequested = false;
        vinRequested = false;
        rtcTimeRequested = false;
        dtcRequested = false;
        readyDevice = null;
        deviceIsVerified = false;
        currentDeviceId = null;
        readyForDeviceTime = true;
    }

    @Override
    public boolean writeRTCInterval(int interval) {
        overWriteInterval = true;
        deviceManager.getSupportedPids();
        debugDrawerInterval = interval;
        return false;
    }

    @Override
    public boolean resetMemory() {
        Log.d(TAG, "resetMemory()");
        deviceManager.clearDeviceMemory();
        return true;
    }

    @Override
    public boolean clearDTCs() {
        deviceManager.clearDtcs();
        return true;
    }

    @Override
    public boolean setChunkSize(int size) {
        pidDataHandler.setChunkSize(size);
        return false;
    }

    @Override
    public void notifyAlarmAdded(Alarm alarm) {
        Log.d(TAG, "notifyAlarmAdded");
        for (Observer o: observerList){
            if (o instanceof AlarmObserver){
                Log.d(TAG, "alarm alarmobserver");
                ((AlarmObserver) o).onAlarmAdded(alarm);
            }
        }
    }

    @Override
    public void alarmEvent(Alarm alarm) {
        alarmHandler.handleAlarm(alarm);
    }

    @Override
    public void idrFuelEvent(String scannerID, double fuelConsumed) {
        Log.d(TAG, "myScannerId is: " + scannerID);
        fuelHandler.handleFuelUpdate(scannerID, fuelConsumed);



    }

    @Override
    public void notifyFuelConsumedUpdate(double fuelConsumed) {
        for (Observer o: observerList){
            if (o instanceof FuelObserver){
                ((FuelObserver) o).onFuelConsumedUpdated();
            }
        }

    }

    @Override
    public void getSupportedPids() {
        deviceManager.getSupportedPids();
    }

    private void trackIdrPidData(PidPackage pidPackage){
        Log.d(TAG,"trackIdrPidData() allowTracking ? "+allowPidTracking);
        if (allowPidTracking){
            allowPidTracking = false;
            try{
                JSONObject properties = new JSONObject();
                if (pidPackage == null){
                    properties.put("pids","null");
                }
                else{
                    properties.put("deviceId",pidPackage.getDeviceId());
                    properties.put("pids",pidPackage.getPids().toString());
                    if (pidPackage instanceof CastelPidPackage){
                        CastelPidPackage castelPidPackage = (CastelPidPackage) pidPackage;
                        properties.put("rtcTime",castelPidPackage.getRtcTime());
                    }

                }
            }catch(JSONException e){
                e.printStackTrace();
            }
            pidTrackTimeoutTimer.start();
        }
    }

    @Override
    public boolean requestDescribeProtocol() {
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceManager != null
                && getDeviceType() == BluetoothDeviceManager.DeviceType.ELM327){
            deviceManager.requestDescribeProtocol();
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean request2141PID() {
        Log.d(TAG,"request2141PID");
        if (deviceManager != null){
            deviceManager.request2141PID();
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean requestStoredDTC() {
        Log.d(TAG,"requestStoredDTC");
        if (deviceManager != null){
            deviceManager.requestStoredDTC();
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean requestPendingDTC() {
        Log.d(TAG,"requestPendingDTC");
        if (deviceManager != null){
            deviceManager.requestPendingDTC();
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean requestSelectProtocol(ObdProtocols p) {
        Log.d(TAG,"requestSelectProtocol() protocol: "+p);
        if (deviceManager != null
                && getDeviceType() == BluetoothDeviceManager.DeviceType.ELM327){
            deviceManager.requestSelectProtocol(p);
            return true;
        }else{
            return false;
        }
    }


    private void setConnectionState(String deviceConnState){
        Logger.getInstance().logI(TAG,"Connection status change: "+deviceConnState, DebugMessage.TYPE_BLUETOOTH);
        this.deviceConnState = deviceConnState;
    }

    @Override
    public void disconnect() {
        deviceManager.setState(IBluetoothCommunicator.DISCONNECTED);

    }
}

package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.bluetooth.handler.BluetoothDataHandlerManager;
import com.pitstop.bluetooth.handler.DtcDataHandler;
import com.pitstop.bluetooth.handler.FreezeFrameDataHandler;
import com.pitstop.bluetooth.handler.PidDataHandler;
import com.pitstop.bluetooth.handler.RtcDataHandler;
import com.pitstop.bluetooth.handler.TripDataHandler;
import com.pitstop.bluetooth.handler.VinDataHandler;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.BluetoothDtcObserver;
import com.pitstop.observer.BluetoothPidObserver;
import com.pitstop.observer.BluetoothVinObserver;
import com.pitstop.observer.ConnectionStatusObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.observer.DeviceVerificationObserver;
import com.pitstop.observer.Observer;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NotificationsHelper;
import com.pitstop.utils.TimeoutTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener
        , BluetoothConnectionObservable, ConnectionStatusObserver, BluetoothDataHandlerManager
        , DeviceVerificationObserver {

    private static final String TAG = BluetoothAutoConnectService.class.getSimpleName();

    private static String SYNCED_DEVICE = "SYNCED_DEVICE";
    private static String DEVICE_ID = "deviceId";

    private final IBinder mBinder = new BluetoothBinder();
    private BluetoothDeviceManager deviceManager;

    private String deviceConnState = State.DISCONNECTED;
    private boolean allPidRequested = false;
    private boolean dtcRequested = false;

    public static int notifID = 1360119;
    private String currentDeviceId = "";

    private MixpanelHelper mixpanelHelper;
    private SharedPreferences sharedPreferences;

    // queue for sending trip flags
    private boolean vinRequested = false;
    private boolean deviceIsVerified = false;
    private boolean ignoreVerification = false; //Whether to begin verifying device by VIN or not
    boolean deviceIdOverwriteInProgress= false;
    private ReadyDevice readyDevice = null;
    private String readDeviceId = "";

    private List<Observer> observerList = new ArrayList<>();

    //Data is passed down to these fellas so they can deal with it
    private PidDataHandler pidDataHandler;
    private DtcDataHandler dtcDataHandler;
    private TripDataHandler tripDataHandler;
    private VinDataHandler vinDataHandler;
    private RtcDataHandler rtcDataHandler;
    private FreezeFrameDataHandler freezeFrameDataHandler;

    private UseCaseComponent useCaseComponent;

    /**
     * for periodic bluetooth scans
     */
    private Handler handler = new Handler();

    private BluetoothServiceBroadcastReceiver connectionReceiver
            = new BluetoothServiceBroadcastReceiver(this);

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    /**Request DTC Data **/
    private Set<String> requestedDtc;
    private final int DTC_RETRY_LEN = 5;
    private final int DTC_RETRY_COUNT = 4;
    private TimeoutTimer dtcTimeoutTimer = new TimeoutTimer(DTC_RETRY_LEN,DTC_RETRY_COUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"dtcTimeoutTimer.onRetry() dtcRequested? "+dtcRequested);
            if (!dtcRequested) return;
            deviceManager.getDtcs();
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"dtcTimeoutTimer.onTimeout() dtcRequested? "+dtcRequested
                    +" found dtc: "+requestedDtc);
            if (!dtcRequested) return;
            if (requestedDtc == null) notifyErrorGettingDtcData();
            else notifyDtcData(requestedDtc);
        }
    };

    /**Request All PID Data **/
    private final int PID_RETRY_LEN = 5;
    private final int PID_RETRY_COUNT = 0;
    private TimeoutTimer pidTimeoutTimer = new TimeoutTimer(PID_RETRY_LEN,PID_RETRY_COUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"pidTimeoutTimer.onRetry() allPidRequested? "+allPidRequested);
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"dtcTimeoutTimer.onTimeout() allPidRequested?"+allPidRequested);
            //Pid data wasn't sent before timer is done
            if (allPidRequested) notifyErrorGettingAllPid();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getApplicationContext()))
                .build();
        mixpanelHelper = new MixpanelHelper((GlobalApplication)getApplicationContext());
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (BluetoothAdapter.getDefaultAdapter() != null) {

            if(deviceManager != null) {
                deviceManager.close();
                deviceManager = null;
            }

            deviceManager = new BluetoothDeviceManager(this);

            deviceManager.setBluetoothDataListener(this);
        }

        registerBroadcastReceiver();

        this.pidDataHandler = new PidDataHandler(this,getApplicationContext());
        this.dtcDataHandler = new DtcDataHandler(this,getApplicationContext());
        this.tripDataHandler = new TripDataHandler(this,this);
        this.vinDataHandler = new VinDataHandler(this,this,this);
        this.rtcDataHandler = new RtcDataHandler(this);
        this.freezeFrameDataHandler = new FreezeFrameDataHandler(this,getApplicationContext());

        //Sometimes terminal time might not be returned
        Runnable periodicGetTerminalTimeRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if (rtcDataHandler.getTerminalRtcTime() == -1
                        && deviceConnState.equals(State.CONNECTED)){
                    Log.d(TAG,"Periodic get terminal time request executing");

                    getObdDeviceTime();
                }
                handler.postDelayed(this, 60000);
            }
        };

        //Sometimes vin might not be returned
        Runnable periodicGetVinRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                //Request VIN if verifying or verification is being ignored
                boolean requestVin = deviceConnState.equals(State.VERIFYING) || ignoreVerification;

                if (requestVin){
                    LogUtils.debugLogI(TAG, "Periodic vin request executed."
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceManager.getVin();
                }
                handler.postDelayed(this, 15000);
            }
        };

        handler.postDelayed(periodicGetTerminalTimeRunnable, 10000);
        handler.postDelayed(periodicGetVinRunnable,5000);

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
            unregisterBroadcastReceiver();
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }

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
    public void getBluetoothState(int state) {
        Log.d(TAG,"getBluetoothState: "+state);
        if (state == IBluetoothCommunicator.CONNECTED) {

            LogUtils.debugLogI(TAG, "getBluetoothState() received CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            //Check to make sure were not overriding the state once
            // its already verified and connected
            clearInvalidDeviceData();
            allPidRequested = false;
            if (deviceConnState.equals(State.SEARCHING)
                    || deviceConnState.equals(State.DISCONNECTED)){

                if (!ignoreVerification){
                    deviceConnState = State.VERIFYING;
                    notifyVerifyingDevice();
                }
            }

            //Get VIN to validate car
            requestVin();

            //Get RTC and mileage once connected
            getObdDeviceTime();

            deviceManager.requestData(); //Request data upon connecting

        } else if (state == IBluetoothCommunicator.DISCONNECTED){

            LogUtils.debugLogI(TAG, "getBluetoothState() received NOT CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());


            //Only notify that device disonnected if a verified connection was established previously
            if (deviceIsVerified || !deviceManager.moreDevicesLeft()){
                deviceConnState = State.DISCONNECTED;
                notifyDeviceDisconnected();
                deviceIsVerified = false;
                NotificationsHelper.cancelConnectedNotification(getApplicationContext());
            }
            currentDeviceId = null;
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
    public void trackBluetoothEvent(String event, String scannerId, String vin){
        Log.d(TAG,"trackBluetoothEvent() event: "+event);

        if (scannerId == null) scannerId = "";
        if (vin == null) vin = "";

        mixpanelHelper.trackBluetoothEvent(event,scannerId,vin,deviceIsVerified,deviceConnState
                ,rtcDataHandler.getTerminalRtcTime());
    }

    @Override
    public void trackBluetoothEvent(String event){
        Log.d(TAG,"trackBluetoothEvent() event: "+event);
        if (readyDevice == null){
            mixpanelHelper.trackBluetoothEvent(event,deviceIsVerified,deviceConnState
                    ,rtcDataHandler.getTerminalRtcTime());
        }
        else{
            trackBluetoothEvent(event,readyDevice.getScannerId()
                    ,readyDevice.getVin());
        }
    }

    @Override
    public void onHandlerReadVin(String vin) {
        Log.d(TAG,"onHandlersReadVin() vin: "+vin);
        notifyVin(vin);
    }

    @Override
    public String getDeviceId() {
        Log.d(TAG,"getDeviceId()");
        return readDeviceId;
    }

    @Override
    public long getRtcTime() {
        Log.d(TAG,"getRtcTime()");
        return rtcDataHandler.getTerminalRtcTime();
    }

    @Override
    public void onHandlerVerifyingDevice() {
        Log.d(TAG,"onHandlerVerifyingDevice()");
        deviceConnState = State.VERIFYING;
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
        if (deviceConnState.equals(State.CONNECTED)){
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
    public void setPidsToBeSent(String pids) {
        deviceManager.setPidsToSend(pids);
    }

    @Override
    public boolean requestDtcData() {
        Log.d(TAG,"requestDtcData() dtcRequested? "+dtcRequested);
        if (dtcRequested) return false;

        dtcRequested = true;
        requestedDtc = null;
        dtcTimeoutTimer.start();
        trackBluetoothEvent(MixpanelHelper.BT_DTC_REQUESTED);
        deviceManager.getDtcs();
        return true;
    }

    @Override
    public void requestVin() {
        Log.d(TAG,"requestVin()");
        vinRequested = true;
        deviceManager.getVin();
    }

    @Override
    public boolean requestAllPid() {
        Log.d(TAG,"requestAllPid(), allPidRequested? "+ allPidRequested);
        if (allPidRequested) return false;

        allPidRequested = true;
        pidTimeoutTimer.start();
        deviceManager.requestSnapshot();
        return true;
    }

    @Override
    public void requestDeviceSearch(boolean urgent, boolean ignoreVerification) {
        Log.d(TAG,"requestDeviceSearch(), deviceConnState: "+deviceConnState
                +", ignoreVerification: "+ignoreVerification);
        this.ignoreVerification = ignoreVerification;

        if (deviceConnState.equals(State.CONNECTED)
                || deviceConnState.equals(State.VERIFYING)) return;

        if (deviceManager.startScan(urgent,ignoreVerification)){

            if (urgent){
                trackBluetoothEvent(MixpanelHelper.BT_SCAN_URGENT);
            }
            else{
                trackBluetoothEvent(MixpanelHelper.BT_SCAN_NOT_URGENT);

            }

            deviceConnState = State.SEARCHING;
            notifySearchingForDevice();
            Log.d(TAG,"Started scan");
        }
        else{
            Log.d(TAG,"Scan failed");
        }
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
        LogUtils.LOGD(TAG,"setParameterResponse(), "+responsePackageInfo.toString());

        if(responsePackageInfo.result == 1) {
            // Once device time is reset, store deviceId
            if (responsePackageInfo.deviceId != null && !responsePackageInfo.deviceId.isEmpty()){
                if (responsePackageInfo.deviceId != null
                        && !responsePackageInfo.deviceId.isEmpty()){
                    currentDeviceId = responsePackageInfo.deviceId;
                }
                saveSyncedDevice(responsePackageInfo.deviceId);
            }

        }
        Log.i(TAG, "Setting parameter response on service callbacks - auto-connect service");
    }

    /**
     * Handles trip data containing mileage
     * @param tripInfoPackage
     */
    @Override
    public void tripData(final TripInfoPackage tripInfoPackage) {
        Log.d(TAG,"tripData()");

        deviceManager.requestData();

        if (tripInfoPackage.deviceId != null && !tripInfoPackage.deviceId.isEmpty()){
            currentDeviceId = tripInfoPackage.deviceId;
        }

        tripInfoPackage.deviceId = currentDeviceId;
        tripDataHandler.handleTripData(tripInfoPackage);
    }

    /**
     * Handles the data returned from a parameter query command
     * @param parameterPackage
     */
    @Override
    public void parameterData(ParameterPackage parameterPackage) {
        if (parameterPackage == null) return;
        if (parameterPackage.paramType == null) return;
        if (parameterPackage.value == null) parameterPackage.value = "";

        if (parameterPackage.deviceId != null && !parameterPackage.deviceId.isEmpty()){
            currentDeviceId = parameterPackage.deviceId;
        }

        if (parameterPackage.paramType.equals(ParameterPackage.ParamType.RTC_TIME)){
            try{
                rtcDataHandler.handleRtcData(Long.valueOf(parameterPackage.value)
                        ,parameterPackage.deviceId);
            }
            catch(Exception e){
                e.printStackTrace();
            }

        }
        else if (parameterPackage.paramType.equals(ParameterPackage.ParamType.VIN)){
            vinDataHandler.handleVinData(parameterPackage.value
                    ,currentDeviceId,ignoreVerification);
        }
        else if (parameterPackage.paramType.equals(ParameterPackage.ParamType.SUPPORTED_PIDS)){
            pidDataHandler.handleSupportedPidResult(parameterPackage.value.split(","));
        }
    }

    @Override
    public void idrPidData(PidPackage pidPackage) {
        LogUtils.debugLogD(TAG, "Received idr pid data: "+pidPackage
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        deviceManager.requestData();
        if (pidPackage == null) return;

        //Set device id if its found in pid package
        if (pidPackage.deviceId != null && !pidPackage.deviceId.isEmpty()){
            currentDeviceId = pidPackage.deviceId;
        }

        pidPackage.deviceId = currentDeviceId;
        pidDataHandler.handlePidData(pidPackage);
    }

    @Override
    public void pidData(PidPackage pidPackage) {
        Log.d(TAG,"Received snapshot() pidPackage: "+pidPackage);
        pidTimeoutTimer.cancel();
        notifyGotAllPid(pidPackage);
    }

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        LogUtils.debugLogD(TAG, "DTC data: " + dtcPackage.toString()
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        //Set device id if its found in dtc package
        if (dtcPackage.deviceId != null && !dtcPackage.deviceId.isEmpty()){
            currentDeviceId = dtcPackage.deviceId;
        }

        dtcPackage.deviceId = currentDeviceId;
        dtcDataHandler.handleDtcData(dtcPackage);
        appendDtc(dtcPackage);

    }

    private void appendDtc(DtcPackage dtcPackage){
        //This needs to be called before a return is made otherwise error will be thrown
        if (requestedDtc == null) requestedDtc = new HashSet<>();

        if (dtcPackage == null) return;

        Log.d(TAG,"appendDtc() dtc before appending: "+requestedDtc);
        for (String d: dtcPackage.dtcs){
            if (!requestedDtc.contains(d)){
                requestedDtc.add(d);
            }
        }
        Log.d(TAG,"appendDtc() dtc after appending: "+ requestedDtc);
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

        if (deviceConnState.equals(State.SEARCHING) && !deviceManager.moreDevicesLeft()) {
            deviceConnState = State.DISCONNECTED;
            notifyDeviceDisconnected();
        }

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            // Here's where the app get the device id for the first time

            LogUtils.debugLogD(TAG, "Login package: " + loginPackageInfo.toString(), true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            sharedPreferences.edit().putString("loginInstruction", loginPackageInfo.instruction).apply();

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
        Log.d(TAG,"onVerificationSuccess() vin: "+vin+", deviceId:"+currentDeviceId);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }

        deviceIsVerified = true;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        readyDevice = new ReadyDevice(vin, currentDeviceId, currentDeviceId);
        notifyDeviceReady(vin,currentDeviceId,currentDeviceId);
        getSupportedPids();
    }

    @Override
    public void onVerificationDeviceBrokenAndCarMissingScanner(String vin) {
        Log.d(TAG,"onVerificationDeviceBrokenAndCarMissingScanner() vin: "
                +vin+", deviceId:"+currentDeviceId);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            if (deviceConnState.equals(State.VERIFYING)){
                deviceManager.onConnectedDeviceInvalid();
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        MainActivity.allowDeviceOverwrite = true;
        deviceIsVerified = true;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        deviceManager.onConnectDeviceValid();
        notifyDeviceNeedsOverwrite();
        readyDevice = new ReadyDevice(vin,currentDeviceId,currentDeviceId);
        notifyDeviceReady(vin,currentDeviceId,currentDeviceId);
        sendConnectedNotification();
        getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceBrokenAndCarHasScanner(String vin, String deviceId) {
        Log.d(TAG,"onVerificationDeviceBrokenAndCarHasScanner() vin: "
                +vin+", deviceId:"+deviceId);

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            if (deviceConnState.equals(State.VERIFYING)){
                deviceManager.onConnectedDeviceInvalid();
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        currentDeviceId = deviceId; //Set it to the device id found on back-end
        setDeviceNameAndId(deviceId);
        deviceIdOverwriteInProgress = true;
        deviceIsVerified = true;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        deviceManager.onConnectDeviceValid();
        readyDevice = new ReadyDevice(vin,deviceId,deviceId);
        notifyDeviceReady(vin,deviceId,deviceId);
        sendConnectedNotification();
        getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceInvalid() {
        Log.d(TAG,"onVerificationDeviceInvalid()");

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            if (deviceConnState.equals(State.VERIFYING)){
                deviceManager.onConnectedDeviceInvalid();
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;

        deviceManager.onConnectedDeviceInvalid();

        if (deviceManager.moreDevicesLeft()){
            deviceConnState = State.SEARCHING;
            notifySearchingForDevice();
        }
        else{
            deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
            notifyDeviceDisconnected();
        }
    }

    @Override
    public void onVerificationDeviceAlreadyActive() {
        Log.d(TAG,"onVerificationDeviceAlreadyActive()");

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            if (deviceConnState.equals(State.VERIFYING)){
                deviceManager.onConnectedDeviceInvalid();
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;
        deviceManager.onConnectedDeviceInvalid();

        if (deviceManager.moreDevicesLeft()){
            deviceConnState = State.SEARCHING;
            notifySearchingForDevice();
        }
        else{
            deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
            notifyDeviceDisconnected();
        }
    }

    @Override
    public void onVerificationError() {
        Log.d(TAG,"onVerificationError()");

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            if (deviceConnState.equals(State.VERIFYING)){
                deviceManager.onConnectedDeviceInvalid();
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        currentDeviceId = null;
        deviceIsVerified = false;
        deviceManager.onConnectedDeviceInvalid();

        if (deviceManager.moreDevicesLeft()){
            deviceConnState = State.SEARCHING;
            notifySearchingForDevice();
        }
        else{
            deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
            notifyDeviceDisconnected();
        }
    }

    @Override
    public void onBluetoothOn() {
        Log.d(TAG,"onBluetoothOn()");
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
        Log.d(TAG,"onBluetoothOff()");
        currentDeviceId = null;
        deviceManager.bluetoothStateChanged(BluetoothAdapter.STATE_OFF); //CONTINUE HERE
        deviceManager.close();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(BluetoothAutoConnectService.notifID);
    }

    @Override
    public void onConnectedToInternet(){
        Log.i(TAG, "Sending stored PIDS and DTCS");
        dtcDataHandler.sendLocalDtcs();
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
    public boolean isConnectedTo215(){
        Log.d(TAG,"isConnectedTo215()");
        if (deviceManager.getConnectionState() == BluetoothCommunicator.CONNECTED)
            return deviceManager.isConnectedTo215();
        else
            return false;
    }

    /**
     * Send command to obd device to retrieve the current device time.
     * @see #parameterData(ParameterPackage) for device time returned
     * by obd device.
     */
    public void getObdDeviceTime() {
        Log.d(TAG,"getObdDeviceTime()");
        deviceManager.getRtc();
    }

    public void setDeviceNameAndId(String name){
        Log.d(TAG,"setDeviceNameAndId() name: "+name);
        deviceManager.setDeviceNameAndId(name);
    }

    public void setDeviceId(String id){
        Log.d(TAG,"setDeviceId() id: "+id);
        deviceManager.setDeviceId(id);
    }

    public void resetObdDeviceTime() {
        Log.d(TAG,"resetObdDeviceTime()");
        deviceManager.setRtc(1088804101);
    }

    public void changeSetting(String param, String value){
        Log.d(TAG,"changeSettings() param: "+param+", value: "+value);
        deviceManager.setParam(param, value);
    }

    /**
     * Store info on already synced device to reduce calls
     * to #getObdDeviceTime()
     *
     * @param deviceId The device id of the currently connected obd device
     */
    public void saveSyncedDevice(String deviceId) {
        Log.d(TAG,"saveSyncedDevice() deviceId: "+deviceId);
        SharedPreferences sharedPreferences = this.getSharedPreferences(SYNCED_DEVICE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_ID, deviceId);
        editor.apply();
    }

    public void getSupportedPids(){ // supported pids
        Log.d(TAG,"getSupportedPids()");
        deviceManager.getSupportedPids();
    }

    public void getPids(String pids) {
        Log.d(TAG,"getPids()");
        deviceManager.getPids(pids);
    }


    public void get215RtcAndMileage(){
        Log.i(TAG, "getting RTC and Mileage - ONLY if connected to 215");
        deviceManager.getRtcAndMileage();
    }

    /**
     * Register connectionReceiver with all actions we want to listen to
     */
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);
    }

    /**
     * Unregister when the service is terminated
     */
    private void unregisterBroadcastReceiver() {
        unregisterReceiver(connectionReceiver);
    }

    private void notifyDeviceNeedsOverwrite() {
        Log.d(TAG,"notifyDeviceNeedsOverwrite()");
        for (Observer o : observerList) {
            if (o instanceof Device215BreakingObserver) {
                ((Device215BreakingObserver) o).onDeviceNeedsOverwrite();
            }
        }
    }

    private void notifySearchingForDevice() {
        Log.d(TAG,"notifySearchingForDevice()");
        trackBluetoothEvent(MixpanelHelper.BT_SEARCHING);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onSearchingForDevice();
            }
        }
    }

    private void notifyDeviceReady(String vin, String scannerId, String scannerName) {
        Log.d(TAG,"notifyDeviceReady() vin: "+vin+", scannerId:"+scannerId
                +", scannerName: "+scannerName);
        trackBluetoothEvent(MixpanelHelper.BT_CONNECTED);
        for (Observer observer: observerList){
            ((BluetoothConnectionObserver)observer)
                    .onDeviceReady(new ReadyDevice(vin, scannerId, scannerName));
        }
    }

    private void notifyDeviceDisconnected() {
        Log.d(TAG,"notifyDeviceDisconnected()");
        trackBluetoothEvent(MixpanelHelper.BT_DISCONNECTED);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceDisconnected();
            }
        }
    }

    private void notifyVerifyingDevice() {
        Log.d(TAG,"notifyVerifyingDevice()");
        trackBluetoothEvent(MixpanelHelper.BT_VERIFYING);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceVerifying();
            }
        }
    }

    private void notifySyncingDevice() {
        Log.d(TAG,"notifySyncingDevice()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceSyncing();
            }
        }
    }

    private void notifyDtcData(Set<String> dtc) {
        Log.d(TAG,"notifyDtcData() "+dtc);
        if (!dtcRequested) return;
        dtcRequested = false;

        trackBluetoothEvent(MixpanelHelper.BT_DTC_GOT);
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                ((BluetoothDtcObserver) observer).onGotDtc(dtc);
            }
        }
    }

    private void notifyErrorGettingDtcData() {
        Log.d(TAG,"notifyErrorGettingDtcData()");
        if (!dtcRequested) return;
        dtcRequested = false;

        trackBluetoothEvent(MixpanelHelper.BT_DTC_GOT);
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                ((BluetoothDtcObserver) observer).onErrorGettingDtc();
            }
        }
    }

    private void notifyVin(String vin) {
        Log.d(TAG,"notifyVin() vin: "+vin);
        if (!vinRequested) return;

        vinRequested = false;
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothVinObserver) {
                ((BluetoothVinObserver)observer).onGotVin(vin);
            }
        }
    }

    private void notifyGotAllPid(PidPackage pidPackage){
        Log.d(TAG,"notifyGotAllPid() pidPackage: "+pidPackage);
        if (!allPidRequested) return;
        allPidRequested = false;

        for (Observer observer: observerList){
            if (observer instanceof BluetoothPidObserver){
                ((BluetoothPidObserver)observer).onGotAllPid(pidPackage.pids);
            }
        }
    }

    private void notifyErrorGettingAllPid(){
        Log.d(TAG,"notifyErrorGettingAllPid()");
        if (!allPidRequested) return;
        allPidRequested = false;

        for (Observer observer: observerList){
            if (observer instanceof BluetoothPidObserver){
                ((BluetoothPidObserver)observer).onErrorGettingAllPid();
            }
        }
    }

    private void sendConnectedNotification(){

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                String carName = "Click here to find out more" +
                        car.getYear() + " " + car.getMake() + " " + car.getModel();
                NotificationsHelper.sendNotification(getApplicationContext()
                        ,carName, "Car is Connected");
            }

            @Override
            public void onNoCarSet() {
                NotificationsHelper.sendNotification(getApplicationContext()
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
        tripDataHandler.clearPendingData();
        dtcDataHandler.clearPendingData();
    }
}

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

import com.pitstop.bluetooth.elm.enums.ObdProtocols;
import com.pitstop.models.Alarm;
import com.castel.obd.info.LoginPackageInfo;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.castel.obd.info.ResponsePackageInfo;
import com.continental.rvd.mobile_sdk.BindingQuestion;
import com.continental.rvd.mobile_sdk.EBindingQuestionType;
import com.continental.rvd.mobile_sdk.SDKIntentService;
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator;
import com.pitstop.bluetooth.communicator.ObdManager;
import com.pitstop.bluetooth.dataPackages.CastelPidPackage;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
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
import java.util.Random;

import io.reactivex.disposables.Disposable;


/**
 *
 * Service responsible for storing information about the bluetooth state, and
 * broadcasting data from and to the UI and bluetooth device
 *
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
    private boolean vinRequested = false;   //Whether VIN has been requested and has not been returned yet
    private boolean deviceIsVerified = false;   //Whether the device has finished and passed the verification process
    private boolean ignoreVerification = false; //Whether to begin verifying device by VIN or not
    private boolean rtcTimeRequested = false;   //Whether the RTC time has been requested and has not been returned yet
    private boolean allowPidTracking = true;    //Whether the number of pids sent to the server and received from the device should be logged
    private boolean allPidRequested = false;    //Whether pids have been requested from the device and have not been returned yet
    private boolean dtcRequested = false;       //Whether dtcs have been requested from the device and habe not been returned yet
    private boolean receivedDtcResponse = false;    //Whether a dtc response has been received at least once from a chain of requests
    private boolean readyForDeviceTime = true;      //Whether the device clock sync use case is ready for execution

    //Connection state values
    private long terminalRtcTime = -1;                      //The rtc time most recently received from the device
    private String currentDeviceId = "";                    //The id of the device most recently connected to
    private String deviceConnState = State.DISCONNECTED;    //The current connection state


    //Data is passed down to these fellas so they can deal with it
    private PidDataHandler pidDataHandler;                  //Handles pid data and executes related use cases
    private DtcDataHandler dtcDataHandler;                  //Handles dtc data and executes related use cases
    private VinDataHandler vinDataHandler;                  //Handles VIN data and executes related use cases
    private FreezeFrameDataHandler freezeFrameDataHandler;  //Handles freeze frame data and executes related use cases

    //Other useful objects
    private final IBinder mBinder = new BluetoothBinder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());    //Handler which runs on the main thread
    private final Handler backgroundHandler = new Handler();                    //This does not run on a background thread
    private final BluetoothServiceBroadcastReceiver connectionReceiver
            = new BluetoothServiceBroadcastReceiver(this);  //Receives notifications about the changes in network connection

    private UseCaseComponent useCaseComponent;      //Provides use cases, dagger 2 component
    private ReadyDevice readyDevice;                //Verified device that is currently connected
    private BluetoothDeviceManager deviceManager;   //Device manager
    private DtcPackage requestedDtcs;               //All of the dtcs that have been returned since the request
    private List<Observer> observerList = Collections.synchronizedList(new ArrayList<>());  //List of observers listening for bluetooth related events
    private AlarmHandler alarmHandler;              //Handles alarm data and executes related use cases
    private FuelHandler fuelHandler;                //Handles fuel data and executes related use cases

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
            if (!vinRequested || deviceManager == null) return;
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
            if (!dtcRequested || deviceManager == null) return;
            deviceManager.getDtcs();
        }

        @Override
        public void onTimeout() {
            if (!dtcRequested || deviceManager == null) return;
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
            if (requestVin && deviceManager != null){
                Log.d(TAG,"Periodic VIN request.");
                deviceManager.getVin();
            }
            backgroundHandler.postDelayed(this, PERIOD_VIN_LEN);
        }
    };

    Random random = new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

//        final Random random = new Random();
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG,"runnable running!");
//                OBD215PidPackage obd215PidPackage = new OBD215PidPackage("215B002373"
//                        ,"1000","1000",System.currentTimeMillis());
//                obd215PidPackage.addPid("210C",Integer.toString(random.nextInt(100),16));
//                obd215PidPackage.addPid("2103",Integer.toString(random.nextInt(10),16));
//                obd215PidPackage.addPid("214F",Integer.toString(random.nextInt(1000),16));
//                obd215PidPackage.addPid("2100",Integer.toString(random.nextInt(60),16));
//                obd215PidPackage.addPid("2104",Integer.toString(random.nextInt(20),16));
//                obd215PidPackage.addPid("2105",Integer.toString(random.nextInt(300),16));
//                obd215PidPackage.addPid("2106",Integer.toString(random.nextInt(10000),16));
//                obd215PidPackage.addPid("2107",Integer.toString(random.nextInt(100),16));
//                obd215PidPackage.addPid("2108",Integer.toString(random.nextInt(5),16));
//                idrPidData(obd215PidPackage);
//                backgroundHandler.postDelayed(this,8000);
//            }
//        };
//        backgroundHandler.post(runnable);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getBaseContext()))
                .build();

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if(deviceManager != null) {
                deviceManager.close();
                deviceManager = null;
            }
            Disposable d = ((GlobalApplication)getApplication()).getServices()
                    .filter(next -> next instanceof SDKIntentService)
                    . subscribe(next -> {
                    deviceManager = new BluetoothDeviceManager(this,(SDKIntentService) next,this);
            });

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

    /**
     * Invoked by BluetoothDeviceManager once list of bluetooth devices in
     * proximity has been found
     */
    @Override
    public void onDevicesFound() {
        Log.d(TAG, "onDevicesFound()");
        if (deviceConnState.equals(State.SEARCHING)){
            setConnectionState(State.FOUND_DEVICES);
            notifyFoundDevices();
        }
    }

    /**
     *
     * Bluetooth state has changed, this is invoked by the lower level bluetooth classes
     *
     * @param state
     */
    @Override
    public void getBluetoothState(int state) {

        switch(state){
            //Connecting to device
            case IBluetoothCommunicator.CONNECTING:
                Log.d(TAG,"getBluetoothState() state: connecting");

                if (deviceConnState.equals(State.SEARCHING)
                        || deviceConnState.equals(State.DISCONNECTED)
                        || deviceConnState.equals(State.FOUND_DEVICES)){
                    setConnectionState(State.CONNECTING);
                    notifyDeviceConnecting();
                }

                break;
            //Connected to device but not yet verified
            case IBluetoothCommunicator.CONNECTED:
                Log.d(TAG,"getBluetoothState() state: "+deviceConnState);

                /*Check to make sure were not overriding the state once
                ** its already verified and connected */
                if (!deviceConnState.equals(State.CONNECTED_UNVERIFIED)
                        && !deviceConnState.equals(State.CONNECTED_VERIFIED)
                        && !deviceConnState.equals(State.VERIFYING)){
                    clearInvalidDeviceData();
                    resetConnectionVars();
                    if (deviceManager != null
                            && deviceManager.getDeviceType() == BluetoothDeviceManager.DeviceType.OBD212) //Sync time for 212 devices
                        deviceManager.setRtc(System.currentTimeMillis());
                    requestVin();                //Get VIN to validate car
                    setConnectionState(State.CONNECTED_UNVERIFIED);
                    notifyVerifyingDevice();     //Verification in progress
                    requestDeviceTime();          //Get RTC and mileage once connected
                    if (deviceManager != null){
                        deviceManager.requestData(); //Request data upon connecting
                    }
                }

                break;
            //Disconnected from device
            case IBluetoothCommunicator.DISCONNECTED:
                Log.d(TAG,"getBluetoothState() state: disconnected");
                //Only notify that device disonnected if a verified connection was established previously
                if (deviceManager != null && ((deviceIsVerified && !ignoreVerification) || !deviceManager.moreDevicesLeft())){
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

    /**
     * Observer has subscribed to a set of bluetooth events
     *
     * @param observer observer listening to bluetooth events
     */
    @Override
    public void subscribe(Observer observer) {
        Log.d(TAG,"subscribe()");
        if (!observerList.contains(observer)){
            observerList.add(observer);
        }
    }

    /**
     * Observer has unsubscribed from a set of bluetooth events
     *
     * @param observer observer that no longer wants to listen to bluetooth events
     */
    @Override
    public void unsubscribe(Observer observer) {
        Log.d(TAG,"unsubscribe()");
        if (observerList.contains(observer)){
            observerList.remove(observer);
        }
    }

    /**
     * VIN Handler has received the VIN
     *
     * @param vin VIN that has been received
     */
    @Override
    public void onHandlerReadVin(String vin) {
        Log.d(TAG,"onHandlersReadVin() vin: "+vin);
        notifyVin(vin);
    }

    /**
     * Get the rtc time
     *
     * @return rtc time most recently captured from the device
     */
    @Override
    public long getRtcTime() {
        Log.d(TAG,"getBluetoothDeviceTime()");
        return terminalRtcTime;
    }

    /**
     * Invoked when the handler begins verifying the device
     *
     */
    @Override
    public void onHandlerVerifyingDevice() {
        Log.d(TAG,"onHandlerVerifyingDevice()");
        setConnectionState(State.VERIFYING);
        notifyVerifyingDevice();
    }

    /**
     *
     * @return current bluetooth connection state
     */
    @Override
    public String getDeviceState() {
        Log.d(TAG,"getDeviceState()");
        return deviceConnState;
    }

    /**
     *
     * @return verified device that the app is currently connected to
     */
    @Override
    public ReadyDevice getReadyDevice() {
        Log.d(TAG,"getReadyDevie()");
        if (deviceConnState.equals(State.CONNECTED_VERIFIED)){
            return readyDevice;
        }
        return null;
    }

    /**
     * Set the rtc time of the device to whatever the real time is
     *
     */
    @Override
    public void requestDeviceSync() {
        Log.d(TAG,"requestDeviceSync()");
        if (deviceManager != null){
            deviceManager.setRtc(System.currentTimeMillis());
            notifySyncingDevice();
        }
    }

    /**
     * Specify which pids are to be returned periodically and how often
     *
     * @param pids which pids are to be returned
     * @param timeInterval at what interval the pids should be returned
     */
    @Override
    public void setPidsToBeSent(String pids, int timeInterval) {
        Log.d(TAG,"setPidsToBeSent() pids: "+pids+", timeInterval: "+timeInterval);
        if (deviceManager != null){
            deviceManager.setPidsToSend(pids, timeInterval);
        }
    }

    /**
     * Depending on the vehicle that the device is connected to, set the
     * communication parameters. Specifically how often data is transferred.
     *
     * @return whether the parameters were successfully set
     */
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

    /**
     * Request detected trouble codes from the device
     *
     * @return whether the request succeeded
     */
    @Override
    public boolean requestDtcData() {
        if (dtcRequested || deviceManager == null) return false;
        Logger.getInstance().logI(TAG,"Engine codes requested", DebugMessage.TYPE_BLUETOOTH);

        dtcRequested = true;
        requestedDtcs = null;
        dtcTimeoutTimer.cancel();
        dtcTimeoutTimer.startTimer();
        if (deviceManager != null)
            deviceManager.getDtcs();
        return true;
    }

    /**
     * Request VIN from the device
     *
     * @return whether the request succeeded
     */
    @Override
    public boolean requestVin() {
        if (vinRequested || deviceManager == null) return false;
        Logger.getInstance().logI(TAG,"VIN requested", DebugMessage.TYPE_BLUETOOTH);
        vinRequested = true;

        getVinTimeoutTimer.cancel();
        getVinTimeoutTimer.startTimer();

        deviceManager.getVin();
        return true;
    }

    /**
     * Request all the pids from the device (This is not IDR pids but a seperate request)
     *
     * @return whether the request succeeded
     */
    @Override
    public boolean requestAllPid() {
        if (allPidRequested || deviceManager == null) return false;
        Logger.getInstance().logI(TAG,"All pid requested", DebugMessage.TYPE_BLUETOOTH);

        allPidRequested = true;
        pidTimeoutTimer.cancel();
        pidTimeoutTimer.startTimer();
        deviceManager.requestSnapshot();
        return true;
    }

    /**
     * Request the rtc time of the device
     *
     * @return whether the request succeeded
     */
    @Override
    public boolean requestDeviceTime() {
        if (rtcTimeRequested || deviceManager == null) return false;
        rtcTimeRequested = true;
        Logger.getInstance().logI(TAG,"Rtc time requested", DebugMessage.TYPE_BLUETOOTH);

        rtcTimeoutTimer.cancel();
        rtcTimeoutTimer.startTimer();
        deviceManager.getRtc();
        return true;
    }

    /**
     * Request a search for devices in close proximity. This will automatically
     * trigger a bluetooth connection to an appropriate device.
     * @param urgent whether a connection should occur even if the device is far away
     * @param ignoreVerification whether VIN and device id verification should occur after connecting
     * @return whether the request was successful
     */
    // This method is called 4 or 5 times after the app started
    @Override
    public void requestDeviceSearch(boolean urgent, boolean ignoreVerification
            , DeviceSearchCallback callback){
        Log.d(TAG,"requestDeviceSearch() urgent: "+Boolean.toString(urgent)
                +", ignoreVerification: "+Boolean.toString(ignoreVerification));

        useCaseComponent.getUserCarUseCase().execute(Repository.DATABASE_TYPE.LOCAL, new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                BluetoothDeviceManager.DeviceType deviceType;
                if (car.getScannerId() == null){
                    deviceType = BluetoothDeviceManager.DeviceType.OBD215; //default if none is present
                }
                else if (car.getScannerId().contains("RVD")){
                    deviceType = BluetoothDeviceManager.DeviceType.RVD;
                }else if (car.getScannerId().contains("215B")){
                    deviceType = BluetoothDeviceManager.DeviceType.OBD215;
                }else if (car.getScannerId().contains("212B")){
                    deviceType = BluetoothDeviceManager.DeviceType.OBD212;
                }else if (!car.getScannerId().isEmpty()){
                    deviceType = BluetoothDeviceManager.DeviceType.ELM327;
                }else{
                    deviceType = BluetoothDeviceManager.DeviceType.OBD215; //default if none is present
                }
                callback.onSearchStatus(requestDeviceSearch(urgent,ignoreVerification,deviceType));
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                callback.onSearchStatus(true);
            }

            @Override
            public void onError(RequestError error) {
                callback.onSearchStatus(false);
            }
        });
        //Comment the line below
//        callback.onSearchStatus(true);
    }


    @Override
    public boolean requestDeviceSearch(boolean urgent, boolean ignoreVerification
            , BluetoothDeviceManager.DeviceType deviceType) {
        Log.d(TAG, "requestDeviceSearch() urgent : " + Boolean.toString(urgent)
                + " ignoreVerification: " + Boolean.toString(ignoreVerification)
                +", deviceType: "+deviceType+", device manager null? "+(deviceManager==null));
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

        //Start scan, and if succeeds change the bluetooth state to searching
//        if (deviceManager != null && deviceManager.startScan(urgent,ignoreVerification, deviceType)){
        if (deviceManager != null && deviceManager.startScan(urgent, ignoreVerification, deviceType)){
            Log.d(TAG,"Started scan");
            setConnectionState(State.SEARCHING);
            notifySearchingForDevice();
        }
        else {
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
                currentDeviceId = responsePackageInfo.deviceId;
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

        //Rtc time has been received from device
        if (parameterPackage.paramType.equals(ParameterPackage.ParamType.RTC_TIME)){
            try{
                long rtcTime = Long.valueOf(parameterPackage.value);
                Logger.getInstance().logI(TAG, "Rtc time received: " + parameterPackage.value
                        , DebugMessage.TYPE_BLUETOOTH);
                if (terminalRtcTime == -1) terminalRtcTime = rtcTime;
                notifyRtc(rtcTime);

                //execute device clock sync use case if flag has been reset to true and currently connected to device is verified
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
        //Vin received from device
        else if (parameterPackage.paramType.equals(ParameterPackage.ParamType.VIN)){
            Logger.getInstance().logI(TAG, "Vin retrieval result: " + parameterPackage.value
                    , DebugMessage.TYPE_BLUETOOTH);
            vinDataHandler.handleVinData(parameterPackage.value
                    ,currentDeviceId,ignoreVerification);
        }
        //Supported pids received from device
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

    @Override
    public void handleVinData(String Vin, String deviceID){
        Logger.getInstance().logI(TAG, "Vin retrieval result: " + Vin
                , DebugMessage.TYPE_BLUETOOTH);
        vinDataHandler.handleVinData(Vin
                ,deviceID,ignoreVerification);
    }

    @Override
    public boolean answerBindingQuestion(EBindingQuestionType questionType, String answer){
        if (deviceManager != null)
            return deviceManager.answerBindingQuestion(questionType,answer);
        else return false;
    }

    @Override
    public void onBindingQuestionPrompted(BindingQuestion bindingQuestion){
        Log.d(TAG,"onBindingQuestionPrompted() question: "+bindingQuestion.question);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .   onBindingQuestionPrompted(bindingQuestion));
            }
        }
    }

    public boolean cancelBinding() {
        Log.d(TAG, "cancelBinding()");
        return deviceManager != null && deviceManager.cancelBinding();
    }

    public boolean startBindingProcess() {
        Log.d(TAG, "startBindingProcess()");
        return deviceManager != null && deviceManager.startBinding();
    }

    public boolean startFirmwareInstallation() {
        Log.d(TAG, "startFirmwareInstallation()");
        return deviceManager != null && deviceManager.startFirmwareInstallation();
    }

    @Override
    public void onBindingProgress(Float progress) {
        Log.d(TAG,"onBindingProgress() progress: "+progress);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onBindingProgress(progress));
            }
        }
    }

    @Override
    public void onBindingFinished() {
        Log.d(TAG,"onBindingFinished()");
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onBindingFinished());
            }
        }
    }

    @Override
    public void onBindingError(Error error) {
        Log.d(TAG,"onBindingError() error: "+error);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onBindingError(error));
            }
        }
    }

    @Override
    public void onFirmwareInstallationRequired() {
        Log.d(TAG,"onFirmwareInstallationRequired()");
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onFirmwareInstallationRequired());
            }
        }
    }

    @Override
    public void onFirmwareInstallationProgress(Float progress) {
        Log.d(TAG,"onFirmwareInstallationProgress() progress: "+progress);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onFirmwareInstallationProgress(progress));
            }
        }
    }

    @Override
    public void onFirmwareInstallationFinished() {
        Log.d(TAG,"onFirmwareInstallationFinished()");
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onFirmwareInstallationFinished());
            }
        }
    }

    @Override
    public void onFirmwareInstallationError(Error error) {
        Log.d(TAG,"onFirmwareInstallationError() error: "+error);
        for (Observer o: observerList ){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> ((BluetoothConnectionObserver)o)
                        .onFirmwareInstallationError(error));
            }
        }
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

    /**
     * Invoked by the lower level bluetooth classes to pass idr pid data
     *
     * @param pidPackage pid data received from the device periodically
     */
    @Override
    public void idrPidData(PidPackage pidPackage) {
        Logger.getInstance().logD(TAG, "IDR pid data received: " + (pidPackage == null ? "null" : pidPackage.toString())
                , DebugMessage.TYPE_BLUETOOTH);

        if (deviceManager != null) deviceManager.requestData();
        trackIdrPidData(pidPackage);

        //Set device id if its found in pid package
        if (pidPackage != null && pidPackage.getDeviceId() != null && !pidPackage.getDeviceId().isEmpty()){
            Log.d(TAG, "setting current device ID to " + pidPackage.getDeviceId());
            currentDeviceId = pidPackage.getDeviceId();
        }
        pidPackage.setDeviceId(currentDeviceId);
        pidDataHandler.handlePidData(pidPackage, vinDataHandler.getRecentVin());

        //Broadcast to all observers
        for (Observer o: observerList){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> {
                    ((BluetoothConnectionObserver)o).onGotPid(pidPackage);
                });
            }
        }

        //212 pid "snapshot" broadcast logic
        if (deviceManager != null && pidPackage != null
                && deviceManager.getDeviceType() == BluetoothDeviceManager.DeviceType.OBD212){
            Log.d(TAG ,"deviceManager is not connected to 215");
            if (pidPackage.getPids() == null){
                pidPackage.setPids(new HashMap<>());
                notifyGotAllPid(pidPackage);
            }else{
                notifyGotAllPid(pidPackage);
            }
        }
    }

    /**
     *
     * @param pidPackage pid data received from the device as a result of a all pid request
     */
    @Override
    public void pidData(PidPackage pidPackage) {
        if (pidPackage == null)return;

        pidTimeoutTimer.cancel();
        Logger.getInstance().logI(TAG, "All pid data received: " + pidPackage.toString()
                , DebugMessage.TYPE_BLUETOOTH);
        notifyGotAllPid(pidPackage);
    }

    /**
     *
     * @param dtcPackage engine trouble codes received from the device
     */
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

    /**
     * Change the device id
     *
     * @param deviceName device id to be set
     */
    @Override
    public void setDeviceName(String deviceName) {
        Log.d(TAG, "setDeviceName: " +deviceName);
        this.ElmMacAddress =deviceName;
        currentDeviceId = deviceName;
    }

    /**
     *
     * @param ffPackage Freeze frame data
     */
    @Override
    public void onBindingRequired() {
        for (Observer o: observerList){
            if (o instanceof BluetoothConnectionObserver){
                mainHandler.post(() -> {
                    ((BluetoothConnectionObserver) o).onBindingRequired();
                });
            }
        }
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

    /**
     * Invoked when the bluetooth device search is completed
     */
    @Override
    public void scanFinished() {

        Log.d(TAG, "scanFinished(), deviceConnState: " + deviceConnState
                + ", deviceManager.moreDevicesLeft?" + deviceManager.moreDevicesLeft());
        if (deviceConnState.equals(State.SEARCHING)
                || deviceConnState.equals(State.FOUND_DEVICES)){
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

            if (deviceManager != null)
                deviceManager.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);

        } else if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }
    }

    /**
     * Vin verification succeeded
     *
     * @param vin vin of vehicle connected to
     */
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
        if (deviceManager != null){
            deviceManager.getSupportedPids();
        }
    }

    /**
     * Device is broken and car does not have a scanner on the server side
     * therefore no correct device id can be written to it
     *
     * @param vin vin of the device
     */
    @Override
    public void onVerificationDeviceBrokenAndCarMissingScanner(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to broken device, car has no scanner",
                DebugMessage.TYPE_BLUETOOTH);
        if (deviceManager == null) return;
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

    /**
     * Device is broken but the server side has the device id stored so
     * it can be written to the device so that it is fixed
     *
     * @param vin vin of the vehicle
     * @param deviceId device id found on the server side, used to override broken device id currently stored
     */
    @Override
    public void onVerificationDeviceBrokenAndCarHasScanner(String vin, String deviceId) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to broken device, car has scanner",
                DebugMessage.TYPE_BLUETOOTH);

        if (deviceManager == null) return;

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

    /**
     * Device that was connected to failed the VIN and device id verification process
     * and should be disconnected from
     *
     * @param vin vin of the vehicle
     */
    @Override
    public void onVerificationDeviceInvalid(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to invalid device",
                DebugMessage.TYPE_BLUETOOTH);

        if (deviceManager == null) return;

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

    /**
     * Device that was connected to is already active on another vehicle
     *
     * @param vin vin of the vehicle
     */
    @Override
    public void onVerificationDeviceAlreadyActive(String vin) {
        Logger.getInstance().logI(TAG,"VIN verification failed due to device being already active",
                DebugMessage.TYPE_BLUETOOTH);

        if (deviceManager == null) return;

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

    /**
     * Error occurred during the verification process
     *
     * @param vin vin of the vehicle
     */
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

    /**
     * Bluetooth has been turned on
     */
    @Override
    public void onBluetoothOn() {
        Logger.getInstance().logI(TAG,"Bluetooth OFF",
                DebugMessage.TYPE_BLUETOOTH);
        if (deviceManager != null) {
            deviceManager.close();
        }

        Disposable d = ((GlobalApplication)getApplication()).getServices()
                .filter(next -> next instanceof SDKIntentService)
                .subscribe(next -> {
                    deviceManager = new BluetoothDeviceManager(this,(SDKIntentService) next,this);
                    if (BluetoothAdapter.getDefaultAdapter()!=null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {

                        requestDeviceSearch(true, false, success -> {

                        }); // start search when turning bluetooth on
                    }
                });
    }

    /**
     * Bluetooth has been turned off
     */
    @Override
    public void onBluetoothOff() {
        Logger.getInstance().logI(TAG,"Bluetooth OFF",
                DebugMessage.TYPE_BLUETOOTH);
        Log.d(TAG,"onBluetoothOff()");
        setConnectionState(State.DISCONNECTED);
        notifyDeviceDisconnected();
        currentDeviceId = null;
        if (deviceManager != null){
            deviceManager.bluetoothStateChanged(BluetoothAdapter.STATE_OFF); //CONTINUE HERE
            deviceManager.close();
        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(BluetoothService.notifID);
    }

    /**
     * Internet connection has been established
     */
    @Override
    public void onConnectedToInternet(){
        Log.i(TAG, "Sending stored PIDS and DTCS");
        dtcDataHandler.sendLocalDtc();
    }

    /**
     * Request freeze frame from the device
     */
    @Override
    public void requestFreezeData() {
        Log.d(TAG,"requestFreezeData()");
        if (deviceManager != null){
            deviceManager.getFreezeFrame();
        }
    }

    /**
     * Check if the device has been verified successfully
     *
     * @return whether device has been verified
     */
    @Override
    public boolean isDeviceVerified() {
        Log.d(TAG,"isDeviceVerified()");
        return deviceIsVerified;
    }

    /**
     *
     * @return type of device currently connected to
     */
    @Override
    public BluetoothDeviceManager.DeviceType getDeviceType(){
        Log.d(TAG,"getDeviceType");
        if (deviceManager == null) return null;
        return deviceManager.getDeviceType();
    }

    /**
     *
     * @param name the device id and device bluetooth name that the device will register
     */
    public void setDeviceNameAndId(String name){
        Log.d(TAG,"setDeviceNameAndId() name: "+name);
        if (deviceManager != null){
            deviceManager.setDeviceNameAndId(name);
        }
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
        if (deviceManager != null && deviceManager.moreDevicesLeft() && deviceManager.connectToNextDevice()){
            setConnectionState(State.SEARCHING);
            notifySearchingForDevice();
        }else if (deviceManager != null){
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

    /**
     * @param interval how often idr/periodic pids are to be retrieved from device
     * @return
     */
    @Override
    public boolean writeRTCInterval(int interval) {
        overWriteInterval = true;
        if (deviceManager != null){
            deviceManager.getSupportedPids();
        }
        debugDrawerInterval = interval;
        return false;
    }

    /**
     * Reset device memory
     *
     * @return true
     */
    @Override
    public boolean resetMemory() {
        Log.d(TAG, "resetMemory()");
        if (deviceManager != null){
            deviceManager.clearDeviceMemory();
        }
        return true;
    }

    /**
     * Clear all DTC present in vehicle
     *
     * @return true
     */
    @Override
    public boolean clearDTCs() {
        if (deviceManager != null){
            deviceManager.clearDtcs();
        }
        return true;
    }

    /**
     * Sets the size of batches that the pids are partitioned into when they are sent over to the server
     * @param size size of each batch
     * @return false
     */
    @Override
    public boolean setChunkSize(int size) {
        pidDataHandler.setChunkSize(size);
        return false;
    }

    /**
     * Notify all AlarmObserver's which are subscribed that an alarm has been added to the local db
     * @param alarm
     */
    @Override
    public void notifyAlarmAdded(Alarm alarm) {
        Log.d(TAG, "notifyAlarmAdded");
        for (Observer o: observerList){
            if (o instanceof AlarmObserver){
                Log.d(TAG, "alarm alarmobserver");
                mainHandler.post(() -> {
                    ((AlarmObserver) o).onAlarmAdded(alarm);
                });
            }
        }
    }

    /**
     *
     * @param alarm Alarm received from device
     */
    @Override
    public void alarmEvent(Alarm alarm) {
        alarmHandler.handleAlarm(alarm);
    }

    /**
     *
     * @param scannerID Device id
     * @param fuelConsumed Fuel consumed by vehicle read from device
     */
    @Override
    public void idrFuelEvent(String scannerID, double fuelConsumed) {
        Log.d(TAG, "myScannerId is: " + scannerID);
        fuelHandler.handleFuelUpdate(scannerID, fuelConsumed);
    }

    /**
     *
     * @param fuelConsumed Fuel consumed by vehicle read from device
     */
    @Override
    public void notifyFuelConsumedUpdate(double fuelConsumed) {
        for (Observer o: observerList){
            if (o instanceof FuelObserver){
                mainHandler.post(() -> {
                    ((FuelObserver) o).onFuelConsumedUpdated();
                });

            }
        }

    }

    /**
     * Request which pids are available for retrieval through the device
     */
    @Override
    public void getSupportedPids() {
        if (deviceManager != null){
            deviceManager.getSupportedPids();
        }
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

    /**
     * Request the protocol currently being used by the ELM327 device for communication
     *
     * @return true if currently connected to ELM327 device, and false otherwise
     */
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

    /**
     * Request 2141 pid from device (Used for emissions)
     * @return true if a Bluetooth Device Manager is present
     */
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

    /**
     * Request the stored DTCs on the vehicle through the device
     *
     * @return true if a Bluetooth Device Manager is present
     */
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

    /**
     * Request pending DTCs on the vehicle through the device
     *
     * @return true if a Bluetooth Device Manager is present
     */
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

    /**
     * Select a protocol to be used by the ELM327 device
     *
     * @param p protocol to be used by the ELM327 device
     * @return true if a verified connection with a ELM327 device is present, and false otherwise
     */
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
        if (deviceManager != null){
            deviceManager.setState(IBluetoothCommunicator.DISCONNECTED);
        }
    }
}

package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.BluetoothDtcObserver;
import com.pitstop.observer.BluetoothVinObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.observer.Observer;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener
        , BluetoothConnectionObservable, ConnectionStatusObserver, BluetoothMixpanelTracker
        , DeviceVerificationObserver{

    private static final String TAG = BluetoothAutoConnectService.class.getSimpleName();
    private static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT);

    private static String SYNCED_DEVICE = "SYNCED_DEVICE";
    private static String DEVICE_ID = "deviceId";
    private static String DEVICE_IDS = "deviceIds";

    private final IBinder mBinder = new BluetoothBinder();
    private BluetoothDeviceManager deviceManager;

    private GlobalApplication application;

    private String deviceConnState = State.DISCONNECTED;

    public static int notifID = 1360119;
    private String currentDeviceId = null;

    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;

    private SharedPreferences sharedPreferences;
    private LocalCarAdapter carAdapter;

    private int lastTripId = -1; // from backend

    // queue for sending trip flags
    private boolean deviceIsVerified = false;
    private boolean ignoreVerification = false; //Whether to begin verifying device by VIN or not
    boolean verificationInProgress = false;
    boolean deviceIdOverwriteInProgress= false;
    private ReadyDevice readyDevice = null;


    private List<Observer> observerList = new ArrayList<>();

    private PidDataHandler pidDataHandler;
    private DtcDataHandler dtcDataHandler;
    private TripDataHandler tripDataHandler;
    private VinDataHandler vinDataHandler;
    private RtcDataHandler rtcDataHandler;

    /**
     * for periodic bluetooth scans
     */
    private Handler handler = new Handler();

    private BluetoothServiceBroadcastReceiver connectionReceiver
            = new BluetoothServiceBroadcastReceiver(this);


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        application = (GlobalApplication) getApplicationContext();

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
        networkHelper = tempNetworkComponent.networkHelper();

        mixpanelHelper = new MixpanelHelper(application);

        if (BluetoothAdapter.getDefaultAdapter() != null) {

            if(deviceManager != null) {
                deviceManager.close();
                deviceManager = null;
            }

            deviceManager = new BluetoothDeviceManager(this);

            deviceManager.setBluetoothDataListener(this);
        }

        carAdapter = new LocalCarAdapter(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        registerBroadcastReceiver();

        this.pidDataHandler = new PidDataHandler(this,getApplicationContext());
        this.dtcDataHandler = new DtcDataHandler(this,getApplicationContext());
        this.tripDataHandler = new TripDataHandler(this,this,getApplicationContext(), handler);
        this.vinDataHandler = new VinDataHandler(this,this,this,this);
        this.rtcDataHandler = new RtcDataHandler(this,this);

        Runnable periodScanRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if(deviceConnState.equals(State.DISCONNECTED)) {
                    Log.d(TAG, "Running periodic scan");
                    startBluetoothSearch(false); // periodic scan
                }
                handler.postDelayed(this, 300000); //Evert 5 minutes
            }
        };

//        Runnable periodicGetSupportedPidsRunnable = new Runnable() { // start background search
//            @Override
//            public void run() { // this is for auto connect for bluetooth classic
//                if(deviceConnState.equals(State.CONNECTED)) {
//                    Log.d(TAG, "Running periodic getSupportedPids()");
//                    if (supportedPids.equals("")){
//                        getSupportedPids(); // periodic scan
//                    }
//                    else{
//                        deviceManager.setPidsToSend(supportedPids);
//                    }
//                }
//                handler.postDelayed(this, 300000); //Evert 5 minutes
//            }
//        };

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
                    getVinFromCar();
                }
                handler.postDelayed(this, 15000);
            }
        };

        handler.post(periodScanRunnable);
       // handler.post(periodicGetSupportedPidsRunnable);
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

    private void cancelConnectedNotification(){
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notifID);

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
            if (deviceConnState.equals(State.SEARCHING)
                    || deviceConnState.equals(State.DISCONNECTED)){

                if (!ignoreVerification){
                    deviceConnState = State.VERIFYING;
                    notifyVerifyingDevice();
                }

            }

            //Get VIN to validate car
            getVinFromCar();

            //Get RTC and mileage once connected
            getObdDeviceTime();

            deviceManager.requestData(); //Request data upon connecting

        } else if (state == IBluetoothCommunicator.DISCONNECTED){

            LogUtils.debugLogI(TAG, "getBluetoothState() received NOT CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());


            /**
             * Set device connection state for connected car indicator,
             * once bluetooth connection is lost.
             * @see MainActivity#connectedCarIndicator()
             * */

            //Only notify that device disonnected if a verified connection was established previously
            if (deviceIsVerified || !deviceManager.moreDevicesLeft()){
                deviceConnState = State.DISCONNECTED;
                notifyDeviceDisconnected();
                deviceIsVerified = false;
                cancelConnectedNotification();
            }
            currentDeviceId = null;
        }
    }

    @Override
    public void subscribe(Observer observer) {
        if (!observerList.contains(observer)){
            observerList.add(observer);
        }
    }

    @Override
    public void unsubscribe(Observer observer) {
        if (observerList.contains(observer)){
            observerList.remove(observer);
        }
    }

    @Override
    public void notifyDeviceNeedsOverwrite() {
        for (Observer o : observerList) {
            if (o instanceof Device215BreakingObserver) {
                ((Device215BreakingObserver) o).onDeviceNeedsOverwrite();
            }
        }
    }

    @Override
    public void notifySearchingForDevice() {
        Log.d(TAG,"notifySearchingForDevice()");
        trackBluetoothEvent(MixpanelHelper.BT_SEARCHING);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onSearchingForDevice();
            }
        }
    }

    @Override
    public void notifyDeviceReady(String vin, String scannerId, String scannerName) {
        Log.d(TAG,"notifyDeviceReady() vin: "+vin+", scannerId:"+scannerId
                +", scannerName: "+scannerName);
        trackBluetoothEvent(MixpanelHelper.BT_CONNECTED);
        for (Observer observer: observerList){
            ((BluetoothConnectionObserver)observer)
                    .onDeviceReady(new ReadyDevice(vin, scannerId, scannerName));
        }
    }

    @Override
    public void trackBluetoothEvent(String event, String scannerId, String vin){
        if (scannerId == null) scannerId = "";
        if (vin == null) vin = "";

        mixpanelHelper.trackBluetoothEvent(event,scannerId,vin,deviceIsVerified,deviceConnState
                ,rtcDataHandler.getTerminalRtcTime());
    }

    @Override
    public void trackBluetoothEvent(String event){
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
    public void notifyDeviceDisconnected() {
        Log.d(TAG,"notifyDeviceDisconnected()");
        trackBluetoothEvent(MixpanelHelper.BT_DISCONNECTED);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceDisconnected();
            }
        }
    }

    @Override
    public void notifyVerifyingDevice() {
        Log.d(TAG,"notifyVerifyingDevice()");
        deviceConnState = State.VERIFYING;
        trackBluetoothEvent(MixpanelHelper.BT_VERIFYING);
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceVerifying();
            }
        }
    }

    @Override
    public String getDeviceState() {
        return deviceConnState;
    }

    @Override
    public ReadyDevice getReadyDevice() {
        if (deviceConnState.equals(State.CONNECTED)){
            return readyDevice;
        }
        return null;
    }

    @Override
    public void requestDeviceSync() {
        syncObdDevice();
        notifySyncingDevice();
    }

    @Override
    public void notifySyncingDevice() {
        Log.d(TAG,"notifySyncingDevice()");
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceSyncing();
            }
        }
    }

    @Override
    public void notifyDtcData(DtcPackage dtcPackage) {
        Log.d(TAG,"notifyDtcData() "+dtcPackage);
        trackBluetoothEvent(MixpanelHelper.BT_DTC_GOT);
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                ((BluetoothDtcObserver) observer).onGotDtc(dtcPackage);
            }
        }
    }

    @Override
    public void notifyVin(String vin) {
        if (!vinRequested) return;

        vinRequested = false;
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothDtcObserver) {
                ((BluetoothVinObserver)observer).onGotVin(vin);
            }
        }
    }

    @Override
    public void requestDtcData() {
        trackBluetoothEvent(MixpanelHelper.BT_DTC_REQUESTED);
        getDTCs();
    }


    private boolean vinRequested = false;
    @Override
    public void requestVin() {
        if (deviceConnState.equals(State.CONNECTED)){

            Log.d(TAG,"Period vin request executing");
            vinRequested = true;
            getVinFromCar();
        }
    }

    @Override
    public void requestDeviceSearch(boolean urgent, boolean ignoreVerification) {
        Log.d(TAG,"requestDeviceSearch(), deviceConnState: "+deviceConnState
                +", ignoreVerification: "+ignoreVerification);
        this.ignoreVerification = ignoreVerification;
        if (deviceConnState.equals(State.CONNECTED)) return;

        //rssi minimum threshold won't matter, rssi will only be used to prioritize devices
        startBluetoothSearch(urgent);
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
                currentDeviceId = responsePackageInfo.deviceId;
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

        tripDataHandler.handleTripData(tripInfoPackage,isConnectedTo215()
                , rtcDataHandler.getTerminalRtcTime());
    }

    /**
     * Handles the data returned from a parameter query command
     * @param parameterPackage
     */
    @Override
    public void parameterData(ParameterPackage parameterPackage) {
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
            vinDataHandler.handleVinData(parameterPackage.value,parameterPackage.deviceId
                    ,ignoreVerification);
        }
    }

    //Remove data that was acquired from the wrong device during the VIN verification process
    private void clearInvalidDeviceData(){
        pidDataHandler.clearPendingData();
        tripDataHandler.clearPendingData();
        dtcDataHandler.clearPendingData();
    }

    @Override
    public void pidData(PidPackage pidPackage) {
        LogUtils.debugLogD(TAG, "Received pid data: "+pidPackage
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.requestData();
        pidDataHandler.handlePidData(pidPackage);
    }

    /**
     * Process dtc data from obd
     * @param dtcPackage
     */
    @Override
    public void dtcData(DtcPackage dtcPackage) {
        LogUtils.debugLogD(TAG, "DTC data: " + dtcPackage.toString()
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        dtcDataHandler.handleDtcData(dtcPackage);
        notifyDtcData(dtcPackage);
    }

    private List<FreezeFramePackage> pendingFreezeFrames = new ArrayList<>();
    private List<FreezeFramePackage> processedFreezeFrames = new ArrayList<>();

    @Override
    public void ffData(FreezeFramePackage ffPackage) {
        LogUtils.debugLogD(TAG, "ffData() " + ffPackage
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        boolean deviceIdMissing = (ffPackage.deviceId == null || ffPackage.deviceId.isEmpty());

        if (!deviceIsVerified || deviceIdMissing){
            LogUtils.debugLogD(TAG, "FreezeFrane added to pending list, device not verified!"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            pendingFreezeFrames.add(ffPackage);
            return;
        }

        if (!pendingFreezeFrames.contains(ffPackage) && pendingFreezeFrames.size() > 0){
            LogUtils.debugLogD(TAG, "Going through pending freeze frames"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            for (FreezeFramePackage p: pendingFreezeFrames){

                //Set device id if it is missing
                if (p.deviceId == null || p.deviceId.isEmpty()){
                    //ffPackage must have device id otherwise we wouldve returned
                    p.deviceId = ffPackage.deviceId;
                }

                processedFreezeFrames.add(p);
                ffData(p);
            }
            pendingFreezeFrames.removeAll(processedFreezeFrames);
            LogUtils.debugLogD(TAG, "Pending freeze frames size() after removal: "
                    + pendingFreezeFrames.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());

        }

        saveFreezeFrame(ffPackage);
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

    private void saveFreezeFrame(FreezeFramePackage ffPackage){
        networkHelper.postFreezeFrame(ffPackage, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    Log.d("Save FF", requestError.getError());
                    Log.d("Save FF", requestError.getMessage());
                }
            }
        });
    }

    private void sendConnectedNotification(){

        //show a custom notification
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_push);

        final Car connectedCar = carAdapter.getCarByScanner(getCurrentDeviceId());

        String carName = connectedCar == null ? "Click here to find out more" :
                connectedCar.getYear() + " " + connectedCar.getMake() + " " + connectedCar.getModel();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                        .setLargeIcon(icon)
                        .setColor(getResources().getColor(R.color.highlight))
                        .setContentTitle("Car is Connected")
                        .setContentText(carName);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(MainActivity.FROM_NOTIF, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifID, mBuilder.build());
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            // Here's where the app get the device id for the first time

            LogUtils.debugLogD(TAG, "Login package: " + loginPackageInfo.toString(), true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            sharedPreferences.edit().putString("loginInstruction", loginPackageInfo.instruction).apply();

            currentDeviceId = loginPackageInfo.deviceId;
            deviceManager.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);

        } else if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }
    }

    @Override
    public void onVerificationSuccess(String vin, String deviceId) {

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        deviceIsVerified = true;
        verificationInProgress = false;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        readyDevice = new ReadyDevice(vin, deviceId, deviceId);
        notifyDeviceReady(vin,deviceId,deviceId);
        getSupportedPids();
    }

    @Override
    public void onVerificationDeviceBrokenAndCarMissingScanner(String vin, String deviceId) {

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        MainActivity.allowDeviceOverwrite = true;
        deviceIsVerified = true;
        verificationInProgress = false;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        deviceManager.onConnectDeviceValid();
        notifyDeviceNeedsOverwrite();
        readyDevice = new ReadyDevice(vin,deviceId,deviceId);
        notifyDeviceReady(vin,deviceId,deviceId);
        sendConnectedNotification();
        getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceBrokenAndCarHasScanner(String vin, String deviceId) {

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        setDeviceNameAndId(deviceId);
        deviceIdOverwriteInProgress = true;
        deviceIsVerified = true;
        verificationInProgress = false;
        deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
        deviceManager.onConnectDeviceValid();
        readyDevice = new ReadyDevice(vin,deviceId,deviceId);
        notifyDeviceReady(vin,deviceId,deviceId);
        sendConnectedNotification();
        getSupportedPids(); //Get supported pids once verified
    }

    @Override
    public void onVerificationDeviceInvalid() {

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        deviceIsVerified = false;
        verificationInProgress = false;

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

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        deviceIsVerified = false;
        verificationInProgress = false;
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

        //ignore result if verification state changed mid use-case execution
        if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
            verificationInProgress = false;
            return;
        }
        //Connected to device mid use-case execution, return
        else if (ignoreVerification){
            verificationInProgress = false;
            if (deviceConnState.equals(State.VERIFYING)){
                deviceConnState = State.SEARCHING;
            }
            return;
        }

        deviceIsVerified = false;
        verificationInProgress = false;
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

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void startBluetoothSearch(boolean urgent) {
        LogUtils.debugLogD(TAG, "startBluetoothSearch() deviceCOnState: "+deviceConnState
                        + ", urgent?"+urgent,
                true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        if (deviceConnState.equals(State.CONNECTED) || deviceConnState.equals(State.VERIFYING)){
            Log.d(TAG,"startBluetoothSearch() device already connected, returning.");
            return;
        }
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

    /**
     * @return The device id of the currently connected obd device
     */
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    /**
     * Send command to obd device to retrieve vin from the currently
     * connected car.
     *
     * @see #parameterData(ParameterPackage) for info returned
     * on the vin query.
     */
    public void getVinFromCar() {
        LogUtils.debugLogI(TAG, "Calling getCarVIN from Bluetooth auto-connect", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getVin();
    }

    /**
     * Send command to obd device to retrieve the current device time.
     * @see #parameterData(ParameterPackage) for device time returned
     * by obd device.
     */
    public void getObdDeviceTime() {
        LogUtils.debugLogI(TAG, "Getting device time", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getRtc();
    }

    /**
     * Sync obd device time with current mobile device time.
     * On successfully syncing device,  #setParameter() gets called
     *
     * @see #setParameterResponse(ResponsePackageInfo)
     */
    public void syncObdDevice() {
        LogUtils.debugLogI(TAG, "Resetting RTC time", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setRtc(System.currentTimeMillis());
    }

    public void setDeviceNameAndId(String name){
        LogUtils.debugLogI(TAG, "Setting device name and id to "+name, true
                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setDeviceNameAndId(name);
    }

    public void setDeviceId(String id){
        LogUtils.debugLogI(TAG, "Setting device id to ["+id+"]", true
                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setDeviceId(id);
    }

    public void resetObdDeviceTime() {
        LogUtils.debugLogI(TAG, "Setting RTC time to 200x", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setRtc(1088804101);
    }

//    public void setFixedUpload() { // to make result 4 pids send every 10 seconds
//        LogUtils.debugLogI(TAG, "Setting fixed upload parameters", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
//        deviceManager.setPidsToSend("2105,2106,2107,210c,210d,210e,210f,2110,2124,2142");
//    }

    public void changeSetting(String param, String value){
        LogUtils.debugLogI(TAG, "Changing setting with param: " + param + ", value: " + value,
                true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setParam(param, value);
    }

    /**
     * Store info on already synced device to reduce calls
     * to #getObdDeviceTime()
     *
     * @param deviceId The device id of the currently connected obd device
     */
    public void saveSyncedDevice(String deviceId) {
        SharedPreferences sharedPreferences = this.getSharedPreferences(SYNCED_DEVICE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_ID, deviceId);
        editor.apply();
    }

    public void getSupportedPids(){ // supported pids
        LogUtils.debugLogI(TAG, "getting supported PIDs", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getSupportedPids();
    }

    /**
     * Get both pending and stored DTCs
     */
    public void getDTCs() {
        LogUtils.debugLogI(TAG, "calling getting DTCs", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getDtcs();
    }

    public void getFreezeData() {
        LogUtils.debugLogI(TAG, "Getting freeze data", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getFreezeFrame();
    }

    public void getPids(String pids) {
        LogUtils.debugLogI(TAG, "getting pids", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getPids(pids);
    }


    public void get215RtcAndMileage(){
        Log.i(TAG, "getting RTC and Mileage - ONLY if connected to 215");
        deviceManager.getRtcAndMileage();
    }

    @Override
    public void onBluetoothOn() {
        if (deviceManager != null) {
            deviceManager.close();
        }

        deviceManager = new BluetoothDeviceManager(this);

        deviceManager.setBluetoothDataListener(this);
        if (BluetoothAdapter.getDefaultAdapter()!=null
                && BluetoothAdapter.getDefaultAdapter().isEnabled()) {

            startBluetoothSearch(true); // start search when turning bluetooth on
        }
    }

    @Override
    public void onBluetoothOff() {
        deviceManager.bluetoothStateChanged(BluetoothAdapter.STATE_OFF); //CONTINUE HERE
        deviceManager.close();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(BluetoothAutoConnectService.notifID);
    }

    @Override
    public void onConnectedToInternet(){
        Log.i(TAG, "Sending stored PIDS and DTCS");
//        TODO
//        executeTripRequests();
//        for (final Dtc dtc : dtcsToSend) {
//            networkHelper.addNewDtc(dtc.getCarId(), dtc.getMileage(),
//                    dtc.getRtcTime(), dtc.getDtcCode(), dtc.isPending(),
//                    new RequestCallback() {
//                        @Override
//                        public void done(String response, RequestError requestError) {
//                            Log.i(TAG, "DTC added: " + dtc);
//
//                            //INCLUDE THIS IN USE CASE IN LATER REFACTOR
//                            //Send notification that dtcs have been updated
//                            // after last one has been sent
//                            if (dtcsToSend.indexOf(dtc) == dtcsToSend.size()-1){
//                                notifyEventBus(new EventTypeImpl(EventType
//                                        .EVENT_DTC_NEW));
//                            }
//                        }
//                    });
//        }
    }

    public int getLastTripId() {
        return lastTripId;
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

    //Null means not connected
    public Boolean isConnectedTo215(){
        if (deviceManager.getConnectionState() == BluetoothCommunicator.CONNECTED)
            return deviceManager.isConnectedTo215();
        else
            return false;
    }

}

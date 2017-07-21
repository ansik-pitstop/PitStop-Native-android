package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
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
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalPidAdapter;
import com.pitstop.database.LocalPidResult4Adapter;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.Trip215EndUseCase;
import com.pitstop.interactors.other.Trip215StartUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Dtc;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.Pid;
import com.pitstop.models.TripEnd;
import com.pitstop.models.TripIndicator;
import com.pitstop.models.TripStart;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothCarObserver;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.observer.Observer;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener
        , BluetoothConnectionObservable {

    private static final String TAG = BluetoothAutoConnectService.class.getSimpleName();
    private static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT);

    private static String SYNCED_DEVICE = "SYNCED_DEVICE";
    private static String DEVICE_ID = "deviceId";
    private static String DEVICE_IDS = "deviceIds";

    public static final String LAST_RTC = "last_rtc-{car_vin}";
    public static final String LAST_MILEAGE = "mileage-{car_vin}";

    private static final int TRIP_END_DELAY = 5000;

    private final IBinder mBinder = new BluetoothBinder();
    private BluetoothDeviceManager deviceManager;
    private List<ObdManager.IBluetoothDataListener> callbacks = new ArrayList<>();

    private GlobalApplication application;

    private boolean isGettingVin = false;
    private boolean gettingPIDs = false;
    private boolean gettingPID = false;
    private boolean deviceConnState = false;

    private BluetoothDeviceRecognizer mBluetoothDeviceRecognizer;

    public static int notifID = 1360119;
    private String currentDeviceId = null;
    private DataPackageInfo lastData = null;

    private int counter = 1;
    private int status5counter = 0;

    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;

    private String[] pids = new String[0];
    private int pidI = 0;

    private LocalPidAdapter localPid;
    private LocalPidResult4Adapter localPidResult4;
    private static final int PID_CHUNK_SIZE = 200;

    private String lastDataNum = "";

    private SharedPreferences sharedPreferences;
    private LocalCarAdapter carAdapter;
    private LocalScannerAdapter scannerAdapter;

    private UseCaseComponent useCaseComponent;

    private ArrayList<Pid> pidsWithNoTripId = new ArrayList<>();

    private int lastDeviceTripId = -1; // from device
    private final String pfDeviceTripId = "lastDeviceTripId";
    private int lastTripId = -1; // from backend
    private final String pfTripId = "lastTripId";
    private int lastTripMileage = 0;
    private final String pfTripMileage = "lastTripMileage";

    private ArrayList<Dtc> dtcsToSend = new ArrayList<>();

    public boolean manuallyUpdateMileage = false;

    // queue for sending trip flags
    final private LinkedList<TripIndicator> tripRequestQueue = new LinkedList<>();
    private boolean isSendingTripRequest = false;
    private boolean deviceIsVerified = false;

    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();
    private String supportedPids = "";
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";

    /**
     * State variable for tracking bluetooth connection time in mixpanel
     */
    private boolean bluetoothConnectedTimeEventStarted = false;
    /**
     * for periodic bluetooth scans
     */
    private Handler handler = new Handler();

    private BluetoothServiceBroadcastReceiver connectionReceiver = new BluetoothServiceBroadcastReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        application = (GlobalApplication) getApplicationContext();

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
        networkHelper = tempNetworkComponent.networkHelper();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        if (BluetoothAdapter.getDefaultAdapter() != null) {

            if(deviceManager != null) {
                deviceManager.close();
                deviceManager = null;
            }

            deviceManager = new BluetoothDeviceManager(this);

            deviceManager.setBluetoothDataListener(this);
            if (BluetoothAdapter.getDefaultAdapter()!=null
                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                startBluetoothSearch(3);  // start search when service starts
            }
        }

        localPid = new LocalPidAdapter(application);
        localPidResult4 = new LocalPidResult4Adapter(application);
        carAdapter = new LocalCarAdapter(application);
        scannerAdapter = new LocalScannerAdapter(application);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        lastTripId = sharedPreferences.getInt(pfTripId, -1);
        lastDeviceTripId = sharedPreferences.getInt(pfDeviceTripId, -1);
        lastTripMileage = sharedPreferences.getInt(pfTripMileage, 0);

        initPidPriorityList();

        registerBroadcastReceiver();

        Runnable periodScanRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if(BluetoothAdapter.getDefaultAdapter().isEnabled() &&
                        deviceManager.getState() == IBluetoothCommunicator.DISCONNECTED) {
                    Log.d(TAG, "Running periodic scan");
                    startBluetoothSearch(4); // periodic scan
                }
                handler.postDelayed(this, 120000);
            }
        };

        //Periodically set fixed upload, temporary solution while queue for set parameters isn't implemented yet
        Runnable periodicSetFixedUploadRunnable = new Runnable() {
            @Override
            public void run(){
                if (deviceIsVerified && deviceManager.getState()
                        == IBluetoothCommunicator.CONNECTED){
                    setFixedUpload();
                }
                handler.postDelayed(this,15000);
            }
        };

        //Sometimes terminal time might not be returned
        Runnable periodicGetTerminalTimeRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if (terminalRTCTime == -1 && deviceManager.getState()
                        == IBluetoothCommunicator.CONNECTED){
                    getObdDeviceTime();
                }
                handler.postDelayed(this, 10000);
            }
        };

        //Sometimes vin might not be returned
        Runnable periodicGetVinRunnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if (!deviceIsVerified && !verificationInProgress && deviceManager.getState()
                        == IBluetoothCommunicator.CONNECTED){
                    LogUtils.debugLogI(TAG, "Periodic vin request executed."
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    getVinFromCar();
                }
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(periodScanRunnable, 15000);
        handler.postDelayed(periodicGetTerminalTimeRunnable, 10000);
        handler.postDelayed(periodicGetVinRunnable,5000);
        handler.postDelayed(periodicSetFixedUploadRunnable, 10000);

        mBluetoothDeviceRecognizer = new BluetoothDeviceRecognizer(this);
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
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void getBluetoothState(int state) {
        if (state == IBluetoothCommunicator.CONNECTED) {

            deviceConnState = true;

            LogUtils.debugLogI(TAG, "getBluetoothState() received CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            //Get VIN to validate car
            getVinFromCar();

            //Get RTC and mileage once connected
            getObdDeviceTime();

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

            // Mixpanel time event
            if (!bluetoothConnectedTimeEventStarted) {
                bluetoothConnectedTimeEventStarted = true;
                mixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_BLUETOOTH_CONNECTED);
            }

        } else {

            LogUtils.debugLogI(TAG, "getBluetoothState() received NOT CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            /**
             * Set device connection state for connected car indicator,
             * once bluetooth connection is lost.
             * @see MainActivity#connectedCarIndicator()
             * */
            deviceConnState = false;

            /**
             * Save current trip data when bluetooth gets disconnected from device
             * @see #processResultFourData(DataPackageInfo)
             */

            if (currentDeviceId != null && lastData != null && !localPidResult4.getAllPidDataEntries().isEmpty()) {
                sendPidDataResult4ToServer(lastData);
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifID);

            // Mixpanel time event
            if (bluetoothConnectedTimeEventStarted) {
                bluetoothConnectedTimeEventStarted = false;
                mixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_BLUETOOTH_CONNECTED);
            }

        }

        Log.i(TAG, "Calling service callbacks to getBluetooth State - auto connect service");

    }

    private List<Observer> observerList = new ArrayList<>();

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
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onSearchingForDevice();
            }
        }
    }

    boolean deviceReady = false;
    @Override
    public void notifyDeviceReady(String vin, String scannerId, String scannerName) {
        deviceReady = true;
        for (Observer observer: observerList){
            ((BluetoothConnectionObserver)observer)
                    .onDeviceReady(vin, scannerId, scannerName);
        }
    }

    @Override
    public void notifyDeviceDisconnected() {
        //Only notify disconnection if the device that was disconnected was verified and ready
        if (deviceReady){
            for (Observer observer: observerList){
                if (observer instanceof BluetoothConnectionObserver){
                    ((BluetoothConnectionObserver)observer).onDeviceDisconnected();
                }
            }
            deviceReady = false;
        }
    }

    @Override
    public void notifyVerifyingDevice() {
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceDisconnected();
            }
        }
    }

    @Override
    public void notifyVIN(String vin, String scannerId){
        for (Observer observer: observerList){
            if (observer instanceof BluetoothCarObserver){
                ((BluetoothCarObserver)observer).onGotVIN(vin, scannerId);
            }
        }
    }

    @Override
    public void requestVIN() {
        getVinFromCar();
    }

    @Override
    public void notifySyncingDevice() {
        for (Observer observer: observerList){
            if (observer instanceof BluetoothConnectionObserver){
                ((BluetoothConnectionObserver)observer).onDeviceVerifying();
            }
        }
    }

    @Override
    public void notifyDtcData(DtcPackage dtcPackage) {
        for (Observer observer : observerList) {
            if (observer instanceof BluetoothCarObserver) {
                ((BluetoothCarObserver) observer).onGotDtc(dtcPackage);
            }
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
            currentDeviceId = responsePackageInfo.deviceId;
            saveSyncedDevice(responsePackageInfo.deviceId);
        }

        Log.i(TAG, "Setting parameter response on service callbacks - auto-connect service");
    }

    private void handle212Trip(TripInfoPackage tripInfoPackage){
        if(manuallyUpdateMileage) {
            Log.i(TAG, "Currently scanning car, ignoring trips");
            return;
        }

        if(tripInfoPackage.tripId != 0) {
            lastDeviceTripId = tripInfoPackage.tripId;
            sharedPreferences.edit().putInt(pfDeviceTripId, lastDeviceTripId).apply();

            if(tripInfoPackage.flag == TripInfoPackage.TripFlag.START) {
                LogUtils.debugLogI(TAG, "Trip start flag received", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            } else if(tripInfoPackage.flag == TripInfoPackage.TripFlag.END) {
                LogUtils.debugLogI(TAG, "Trip end flag received", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                if(lastTripId == -1) {
                    networkHelper.getLatestTrip(tripInfoPackage.deviceId, new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if(requestError == null && !response.equals("{}")) {
                                try {
                                    lastTripId = new JSONObject(response).getInt("id");
                                    sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                    tripRequestQueue.add(new TripEnd(lastTripId, String.valueOf(tripInfoPackage.rtcTime),
                                            String.valueOf(tripInfoPackage.mileage)));
                                    executeTripRequests();
                                } catch(JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    tripRequestQueue.add(new TripEnd(lastTripId, String.valueOf(tripInfoPackage.rtcTime),
                            String.valueOf(tripInfoPackage.mileage)));
                    executeTripRequests();
                }
                Car car = carAdapter.getCarByScanner(tripInfoPackage.deviceId);
                if(car != null) {
                    double newMileage = car.getTotalMileage() + tripInfoPackage.mileage;
                    car.setTotalMileage(newMileage);
                    carAdapter.updateCar(car);
                }
            }
        }
    }

    private long terminalRTCTime = -1;
    private List<TripInfoPackage> pendingTripInfoPackages = new ArrayList<>();

    //Variables below are used for testing
    private boolean registerDummyTripStart = false;
    private boolean registerDummyTripEnd = false;
    private int skipCounter = 0;
    private int dummyTripId = -1;
    private double lastTripEndMileage = -1;

    /**
     * Handles trip data containing mileage
     * @param tripInfoPackage
     */
    @Override
    public void tripData(final TripInfoPackage tripInfoPackage) {

        final String TAG = BluetoothAutoConnectService.class.getSimpleName() + ".tripData()";

        //Not handling trip updates anymore since live mileage has been removed
        if (tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE)){
            LogUtils.debugLogD(TAG, "trip update received, ignoring: "
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            return;
        }

        LogUtils.debugLogD(TAG, "Trip start/end received: " + tripInfoPackage.toString()
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());



        /*********************************TEST CODE START*********************************/

        /*Create dummy start and end trip objects using the received update
        **objects since the simulator only sends update objects*/
//        if (tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE)){
//
//            //We don't want to add to pending trip list if its an update or historical
//            if (terminalRTCTime == -1 || tripInfoPackage.rtcTime < terminalRTCTime) return;
//
//            if (!registerDummyTripStart && skipCounter == 0){
//                if (dummyTripId == -1){
//                    if (isConnectedTo215()){
//                        dummyTripId = tripInfoPackage.tripId;
//                    }
//                    else{
//                        dummyTripId = (int)tripInfoPackage.rtcTime;
//                    }
//                }
//                else{
//                    dummyTripId += 100;
//                }
//
//                //Make sure start mileages make sense, meaning last trip end is this ones start
//                if (lastTripEndMileage != -1 && isConnectedTo215()){
//                    tripInfoPackage.mileage = lastTripEndMileage;
//                }
//                else if (!isConnectedTo215()){
//                    tripInfoPackage.mileage = 0;
//                }
//
//                tripInfoPackage.tripId = dummyTripId;
//                tripInfoPackage.flag = TripInfoPackage.TripFlag.START;
//                tripInfoPackage.rtcTime += 100;
//                registerDummyTripStart = true;
//                registerDummyTripEnd = false;
//                skipCounter = 8;
//                LogUtils.LOGD(TAG,"Created dummy tripInfoPackage: "+tripInfoPackage);
//            }
//            else if (!registerDummyTripEnd && skipCounter == 0){
//
//                tripInfoPackage.tripId = dummyTripId;
//                tripInfoPackage.flag = TripInfoPackage.TripFlag.END;
//                tripInfoPackage.rtcTime += 1000;
//                tripInfoPackage.mileage += 150;
//                lastTripEndMileage = tripInfoPackage.mileage;
//                registerDummyTripEnd = true;
//                registerDummyTripStart = false;
//                skipCounter = 8;
//                LogUtils.LOGD(TAG,"Created dummy tripInfoPackage: "+tripInfoPackage);
//            }
//            //Skip 4 trip updates before create dummy trip start and trip end again
//            else{
//                LogUtils.LOGD(TAG,"Skipping trip update, skips left:"+(skipCounter-1));
//                if (skipCounter > 0){
//                    skipCounter--;
//                }
//                //go back into simulate trip start and trip end mode again
//                return;
//            }
//        }

        /*********************************TEST CODE END**********************************/

        /*Code for handling 212 trip logic, moved to private method since its being
          phased out and won't be maintained*/
        if (!isConnectedTo215() && deviceIsVerified){
            LogUtils.debugLogD(TAG, "handling 212 trip rtcTime:"+tripInfoPackage.rtcTime, true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            handle212Trip(tripInfoPackage);
            return;
        }

        LogUtils.debugLogD(TAG, "Adding trip to pending list"
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        boolean deviceIdMissing = (tripInfoPackage.deviceId == null
                || tripInfoPackage.deviceId.isEmpty());

        //Check to see if we received current RTC time from device upon the app detecting device
        //If not received yet store the trip for once it is received
        pendingTripInfoPackages.add(tripInfoPackage);
        if (terminalRTCTime == -1 || !deviceIsVerified || deviceIdMissing){
            LogUtils.debugLogD(TAG, "Trip added to pending list, terminalRtcSet?"
                            +(terminalRTCTime != -1)+", deviceVerified?"+deviceIsVerified
                            +", deviceIdMissing?"+deviceIdMissing
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            return;
        }

        LogUtils.debugLogD(TAG, "Going through all pending trips tripListSize: "
                +pendingTripInfoPackages.size(), true, DebugMessage.TYPE_BLUETOOTH
                , getApplicationContext());

        //Go through all pending trip info packages including the one just passed in parameter
        List<TripInfoPackage> toRemove = new ArrayList<>();
        for (TripInfoPackage trip: pendingTripInfoPackages){

            /*Set the device id for any trips that were received while a device was broken
            /** prior to an overwrite*/
            boolean tripHasNoId = trip.deviceId == null || trip.deviceId.isEmpty();
            if (tripHasNoId){
                //TripInfoPackage must have trip id or it would have returned due to deviceIdMissing flag
                trip.deviceId = tripInfoPackage.deviceId;
            }

            if (trip.flag.equals(TripInfoPackage.TripFlag.END) && isConnectedTo215()){

                LogUtils.debugLogD(TAG, "Executing trip end use case", true
                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                useCaseComponent.trip215EndUseCase().execute(trip, terminalRTCTime
                        , new Trip215EndUseCase.Callback() {
                    @Override
                    public void onHistoricalTripEndSuccess() {
                        LogUtils.debugLogD(TAG, "Historical trip END saved successfully", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    }

                    @Override
                    public void onRealTimeTripEndSuccess() {
                        LogUtils.debugLogD(TAG, "Real-time END trip end saved successfully", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                        //Send update mileage notification after 5 seconds to allow back-end to process mileage
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                notifyEventBus(new EventTypeImpl(EventType.EVENT_MILEAGE));
                            }
                        },TRIP_END_DELAY);

                    }

                    @Override
                    public void onStartTripNotFound() {
                        LogUtils.debugLogD(TAG, "Trip start not found, mileage will update on "
                                +"next trip start", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    }

                            @Override
                    public void onError() {
                        LogUtils.debugLogD(TAG,"TRIP END Use case returned error", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    }
                });

            }
            else if (trip.flag.equals(TripInfoPackage.TripFlag.START) && isConnectedTo215()){

                LogUtils.debugLogD(TAG, "Executing trip start use case", true
                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                useCaseComponent.trip215StartUseCase().execute(trip, terminalRTCTime
                        , new Trip215StartUseCase.Callback() {
                    @Override
                    public void onRealTimeTripStartSuccess() {
                        LogUtils.debugLogD(TAG, "Real-time trip START saved successfully", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                notifyEventBus(new EventTypeImpl(EventType.EVENT_MILEAGE));
                            }
                        },TRIP_END_DELAY);
                    }

                    @Override
                    public void onHistoricalTripStartSuccess(){
                        LogUtils.debugLogD(TAG, "Historical trip START saved successfully", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    }

                    @Override
                    public void onError() {
                        LogUtils.debugLogD(TAG,"Error saving trip start", true
                                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    }
                });

            }
            toRemove.add(trip);

        }
        pendingTripInfoPackages.removeAll(toRemove);

        LogUtils.debugLogD(TAG, "rtcTime: "+tripInfoPackage.rtcTime
                +" Completed all running use cases on all pending trips"
                +" pendingTripList.size() after removing:"
                +pendingTripInfoPackages.size(), true
                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

    }

    public void connectedDeviceInvalid(){
        deviceManager.onConnectedDeviceInvalid();
    }

    boolean verificationInProgress = false;
    boolean deviceIdOverwriteInProgress= false;

    /**
     * Handles the data returned from a parameter query command
     * @param parameterPackage
     */
    @Override
    public void parameterData(ParameterPackage parameterPackage) {
        if (parameterPackage == null) return;

        final String TAG = getClass().getSimpleName() + ".parameterData()";

        LogUtils.debugLogD(TAG, "addingCar?"+AddCarActivity.addingCarWithDevice+", parameterPackage: " + parameterPackage.toString()
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        //Get terminal RTC time
        if (parameterPackage.paramType == ParameterPackage.ParamType.RTC_TIME
                && terminalRTCTime == -1 && !AddCarActivity.addingCarWithDevice){
            terminalRTCTime = Long.valueOf(parameterPackage.value);
        }

        //Check to see if VIN is correct, unless adding a car then no comparison is needed
        else if(parameterPackage.paramType == ParameterPackage.ParamType.VIN
                && !AddCarActivity.addingCarWithDevice && !verificationInProgress
                && !deviceIsVerified){

            //Notify observers about the received VIN
            notifyVIN(parameterPackage.value, parameterPackage.deviceId);

            verificationInProgress = true;

            useCaseComponent.handleVinOnConnectUseCase().execute(parameterPackage, new HandleVinOnConnectUseCase.Callback() {
                @Override
                public void onSuccess() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect: Success"
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceIsVerified = true;
                    verificationInProgress = false;
                    setFixedUpload();
                    deviceManager.onConnectDeviceValid();
                }

                @Override
                public void onDeviceBrokenAndCarMissingScanner() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device ID needs to be overriden"
                          ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    MainActivity.allowDeviceOverwrite = true;
                    deviceIsVerified = true;
                    verificationInProgress = false;
                    setFixedUpload();
                    deviceManager.onConnectDeviceValid();
                }

                @Override
                public void onDeviceBrokenAndCarHasScanner(String scannerId) {
                    LogUtils.debugLogD(TAG, "Device missing id but user car has a scanner" +
                            ", overwriting scanner id to "+scannerId,true
                            , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    setDeviceNameAndId(scannerId);
                    deviceIdOverwriteInProgress = true;
                    deviceIsVerified = true;
                    verificationInProgress = false;
                    setFixedUpload();
                    deviceManager.onConnectDeviceValid();
                }

                @Override
                public void onDeviceInvalid() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is invalid."
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    clearInvalidDeviceData();
                    deviceIsVerified = false;
                    verificationInProgress = false;
                    deviceManager.onConnectedDeviceInvalid();
                }

                @Override
                public void onDeviceAlreadyActive() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is already active"
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    clearInvalidDeviceData();
                    deviceIsVerified = false;
                    verificationInProgress = false;
                    deviceManager.onConnectedDeviceInvalid();
                }

                @Override
                public void onError() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect error occurred"
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    clearInvalidDeviceData();
                    deviceIsVerified = false;
                    deviceManager.onConnectedDeviceInvalid();
                    verificationInProgress = false;
                }
            });

        }

        if(parameterPackage.paramType == ParameterPackage.ParamType.SUPPORTED_PIDS) {
            Log.d(TAG, "parameterData: " + parameterPackage.toString());
            Log.i(TAG, "Supported pids returned");
            String[] pids = parameterPackage.value.split(","); // pids returned separated by commas
            HashSet<String> supportedPidsSet = new HashSet<>(Arrays.asList(pids));
            StringBuilder sb = new StringBuilder();
            int pidCount = 0;
            // go through the priority list and get the first 10 pids that are supported
            for(String dataType : PID_PRIORITY) {
                if(pidCount >= 10) {
                    break;
                }
                if(supportedPidsSet.contains(dataType)) {
                    sb.append(dataType);
                    sb.append(",");
                    ++pidCount;
                }
            }
            if(sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') { // remove comma at end
                supportedPids = sb.substring(0, sb.length() - 1);
            } else {
                supportedPids = DEFAULT_PIDS;
            }
            deviceManager.setPidsToSend(supportedPids);
        }
    }

    //Remove data that was acquired from the wrong device during the VIN verification process
    private void clearInvalidDeviceData(){
        pendingPidPackages.clear();
        pendingTripInfoPackages.clear();
        pendingDtcPackages.clear();
    }

    //Queue up the pid packages that arrive before VIN is verified
    private List<PidPackage> pendingPidPackages = new ArrayList<>();
    private List<PidPackage> processedPidPackages = new ArrayList<>();

    @Override
    public void pidData(PidPackage pidPackage) {
        LogUtils.debugLogD(TAG, "Received pid data: "+pidPackage
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        boolean deviceIdMissing = pidPackage.deviceId == null || pidPackage.deviceId.isEmpty();

        if (!deviceIsVerified || deviceIdMissing){
            LogUtils.debugLogD(TAG, "Pid data added to pending list, device not verified " +
                    "OR device id is missing.", true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            pendingPidPackages.add(pidPackage);
            return;
        }

        //Check if its a recursive call
        if (!processedPidPackages.contains(pidPackage) && pendingPidPackages.size() > 0){
            LogUtils.debugLogD(TAG, "Device is verified and device id present" +
                    ", going through pid pending list, size: " +pendingPidPackages.size()
                    , true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            //Not a recursive call, go throgh pending pid packages recursively
            for (PidPackage p: pendingPidPackages){

                //Set device id if it is is missing
                if (p.deviceId == null || p.deviceId.isEmpty()){
                    //pidPackage must have device id otherwise we would've returned
                    p.deviceId = pidPackage.deviceId;
                }

                processedPidPackages.add(p);
                pidData(p);
            }

            pendingPidPackages.removeAll(processedPidPackages);
            LogUtils.debugLogD(TAG, "Pid pending list size after removal: "+pendingPidPackages.size()
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        }


        deviceManager.requestData();

        if(pidPackage.realTime) { // notify that real time received
            manuallyUpdateMileage = false;
        }

        if (pidPackage.pids == null || pidPackage.pids.size() == 0) {
            Log.i(TAG, "No pids returned pidPackage:"+pidPackage.toString());
            return;
        }

        if(pidPackage.deviceId != null && !pidPackage.deviceId.isEmpty()) {
            currentDeviceId = pidPackage.deviceId;
        }

        // if trip id is different, start a new trip
        if(!isConnectedTo215() && pidPackage.tripId != null && !pidPackage.tripId.isEmpty() && !pidPackage.tripId.equals("0")
                && carAdapter.getCarByScanner(pidPackage.deviceId) != null) {
            int newTripId = Integer.valueOf(pidPackage.tripId);
            if(newTripId != lastDeviceTripId) {
                lastDeviceTripId = newTripId;
                sharedPreferences.edit().putInt(pfDeviceTripId, newTripId).apply();

                if(lastData != null) {
                    sendPidDataResult4ToServer(lastData);
                }
                tripRequestQueue.add(new TripStart(lastDeviceTripId, pidPackage.rtcTime, pidPackage.deviceId));
                executeTripRequests();
            }
        }

        counter++;

        if(counter==20){
            if(!isGettingVin) {
                getSupportedPids();
            }
        }
        if(counter % 500 == 0){
            getDTCs();
        }
        if(counter == 2000){
            counter = 1;
        }

        Log.d(TAG, "pidData: " + pidPackage.toString());

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        Car car = carAdapter.getCarByScanner(pidPackage.deviceId);

        double mileage;
        double calculatedMileage;

        if(pidPackage.tripMileage != null && !pidPackage.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(pidPackage.tripMileage) / 1000;
            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
        } else if(lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(lastData.tripMileage)/1000;
            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
        } else {
            mileage = 0;
            calculatedMileage = 0;
        }

        pidDataObject.setMileage(mileage); // trip mileage from device
        pidDataObject.setCalculatedMileage(calculatedMileage);
        pidDataObject.setDataNumber(lastDataNum == null ? "" : lastDataNum);
        pidDataObject.setTripId(Long.parseLong(pidPackage.tripId));
        pidDataObject.setRtcTime(pidPackage.rtcTime);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> pidEntry : pidPackage.pids.entrySet()) {
            sb.append(pidEntry.getKey());
            sb.append(": ");
            sb.append(pidEntry.getValue());
            sb.append(" / ");
            try {
                JSONObject pid = new JSONObject();
                pid.put("id", pidEntry.getKey());
                pid.put("data", pidEntry.getValue());
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "PIDs received: " + sb.toString());

        pidDataObject.setPids(pids.toString());

        if(pidDataObject.getMileage() >= 0 && pidDataObject.getCalculatedMileage() >= 0) {
            localPid.createPIDData(pidDataObject);
        }

        if(localPid.getPidDataEntryCount() >= PID_CHUNK_SIZE && localPid.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer(pidPackage.rtcTime, pidPackage.deviceId, pidPackage.tripId);
        }
    }

    private List<DtcPackage> pendingDtcPackages = new ArrayList<>();
    private List<DtcPackage> processedDtcPackages = new ArrayList<>();

    /**
     * Process dtc data from obd
     * @param dtcPackage
     */
    @Override
    public void dtcData(DtcPackage dtcPackage) {
        LogUtils.debugLogD(TAG, "DTC data: " + dtcPackage.toString(), true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        //Waiting for overwrite
        boolean deviceIdMissing = dtcPackage.deviceId == null || dtcPackage.deviceId.isEmpty();

        if (!deviceIsVerified || deviceIdMissing){
            LogUtils.debugLogD(TAG, "Dtc data added to pending list, device not verified!"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            pendingDtcPackages.add(dtcPackage);
            return;
        }

        if (!pendingDtcPackages.contains(dtcPackage) && pendingDtcPackages.size() > 0){
            LogUtils.debugLogD(TAG, "Going through pending dtc packages, length: "
                    +pendingDtcPackages.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            for (DtcPackage p: pendingDtcPackages){

                //Set device id if it is missing
                if (p.deviceId == null || p.deviceId.isEmpty()){

                    //Must be present otherwise we would've returned due to deviceIdMissing flag
                    p.deviceId = dtcPackage.deviceId;
                }

                processedDtcPackages.add(p);
                dtcData(p);
            }
            pendingDtcPackages.removeAll(processedDtcPackages);
            LogUtils.debugLogD(TAG, "Pending dtc packages list length after removal: "
                    +pendingDtcPackages.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
        }


        if(dtcPackage.dtcNumber > 0) {
            saveDtcs(dtcPackage);
            getFreezeData();
        }
    }

    private List<FreezeFramePackage> pendingFreezeFrames = new ArrayList<>();
    private List<FreezeFramePackage> processedFreezeFrames = new ArrayList<>();

    @Override
    public void ffData(FreezeFramePackage ffPackage) {
        LogUtils.debugLogD(TAG, "ffData() " + ffPackage
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        boolean deviceIdMissing = ffPackage.deviceId == null || ffPackage.deviceId.isEmpty();

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

    private void saveDtcs(final DtcPackage dtcPackage) {
        Car car = carAdapter.getCarByScanner(dtcPackage.deviceId);

        if(NetworkHelper.isConnected(this)) {
            if (car != null) {
                networkHelper.getCarsById(car.getId(), new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                Car car = Car.createCar(response);

                                HashSet<String> dtcNames = new HashSet<>();
                                for (CarIssue issue : car.getActiveIssues()) {
                                    dtcNames.add(issue.getItem());
                                }

                                List<String> dtcList = Arrays.asList(dtcPackage.dtcs);
                                List<String> toRemove = new ArrayList<String>();
                                for (String dtc: dtcList){
                                    if (dtcNames.contains(dtc)){
                                        toRemove.add(dtc);
                                    }
                                }
                                dtcList.removeAll(toRemove);

                                for (final String dtc: dtcList) {
                                    final int dtcListSize = dtcList.size();
                                    final List<String> dtcListReference = dtcList;

                                    networkHelper.addNewDtc(car.getId(), car.getTotalMileage(),
                                            dtcPackage.rtcTime, dtc, dtcPackage.isPending,
                                            new RequestCallback() {
                                                @Override
                                                public void done(String response, RequestError requestError) {
                                                    Log.i(TAG, "DTC added: " + dtc);

                                                    //INCLUDE THIS INSIDE USE CASE WHEN REFACTORING
                                                    //Notify that dtcs have been updated once
                                                    // the last one has been sent successfully
                                                    if (dtcListReference.indexOf(dtc)
                                                            == dtcListReference.size()-1){

                                                        notifyEventBus(new EventTypeImpl(
                                                                EventType.EVENT_DTC_NEW));
                                                    }
                                                }
                                            });
                                }
                                carAdapter.updateCar(car);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } else if(car != null) {
            Log.i(TAG, "Saving dtcs offline");
            for (final String dtc : dtcPackage.dtcs) {
                dtcsToSend.add(new Dtc(car.getId(), car.getTotalMileage(), dtcPackage.rtcTime,
                        dtc, dtcPackage.isPending));
            }
        }
    }

    private void notifyEventBus(EventType eventType){
        CarDataChangedEvent carDataChangedEvent
                = new CarDataChangedEvent(eventType,EVENT_SOURCE);
        EventBus.getDefault().post(carDataChangedEvent);
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

    public void saveScannerOnResultPostCar(Car createdCar) {
        Log.d(TAG, "Saving scanner on result post car");

        if (currentDeviceId == null) {
            Log.d(TAG, "Current Device ID is null");
            return;
        }

        String btName = deviceManager.getConnectedDeviceName();

        Log.d(TAG, "Saving device name: " + (btName != null ? btName : "BT DEVICE NAME IS NULL!"));
        Log.d(TAG, "Saving device ID: " + currentDeviceId);
        Log.d(TAG, "Saving car ID: " + (createdCar != null ? createdCar.getId() : "Car retrieved from database using currentDeviceId is null!"));

        if (btName != null && (createdCar != null || AddCarActivity.addingCarWithDevice)) {
            Log.i(TAG, "Saving scanner locally");
            scannerAdapter.storeScanner(new ObdScanner((createdCar != null ? createdCar.getId() : 0), btName, currentDeviceId));
        } else {
            Log.i(TAG, "Connected to unrecognized device");
        }
    }

    /**
     * Populate a row in the local scanner table with only carId
     *
     * @param carId
     */
    public void saveEmptyScanner(int carId) {
        Log.d(TAG, "Saving empty scanner (carId only), carId: " + carId);
        if (scannerAdapter.isCarExist(carId) || carId == 0) {
            return;
        }
        ObdScanner scanner = new ObdScanner();
        scanner.setCarId(carId);
        scanner.setScannerId("");
        scanner.setDeviceName("");
        scanner.setDatanum("");
        scannerAdapter.storeScanner(scanner);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void startBluetoothSearch(int... source) {
        LogUtils.debugLogD(TAG, "startBluetoothSearch() " + ((source != null && source.length > 0) ? source[0] : ""),
                true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.startScan();
    }

    /**
     * @return The connection state of the obd device to the car.
     * If device is sending data packages with result greater than
     * 3, then device is connected
     */
    public boolean isCommunicatingWithDevice() {
        return deviceIsVerified && deviceConnState;
    }

    /**
     * @return The device id of the currently connected obd device
     */
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    /**
     * Gets the Car's VIN. Check if obd device is synced. If synced,
     * send command to device to retrieve vin info.
     *
     * @see #getObdDeviceTime()
     * @see #parameterData(ParameterPackage)
     */
    public void getCarVIN() {
        LogUtils.debugLogI(TAG, "getCarVin", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        isGettingVin = true;
        getObdDeviceTime();
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
        terminalRTCTime = System.currentTimeMillis();
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

    public void setFixedUpload() { // to make result 4 pids send every 10 seconds
        LogUtils.debugLogI(TAG, "Setting fixed upload parameters", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setPidsToSend("2105,2106,2107,210c,210d,210e,210f,2110,2124,2142");
    }

    public void changeSetting(String param, String value){
        LogUtils.debugLogI(TAG, "Changing setting with param: " + param + ", value: " + value,
                true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.setParam(param, value);
    }

    public void resetDeviceToFactory() {
        Log.i(TAG, "Resetting device to factory settings");

        //bluetoothCommunicator.obdSetCtrl(4);
    }

    public void removeSyncedDevice() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(SYNCED_DEVICE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DEVICE_ID);
        editor.apply();
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

    /**
     * @return The device id of the most recently synced obd device
     */
    private String getSavedSyncedDeviceId() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(SYNCED_DEVICE,
                Context.MODE_PRIVATE);
        return sharedPreferences.getString(DEVICE_ID, null);
    }

    public int getState() {
        return deviceManager.getState();
    }


    public void getSupportedPids(){ // supported pids
        LogUtils.debugLogI(TAG, "getting supported PIDs", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getSupportedPids();
        gettingPID = true;
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

    public void clearDTCs() {
        LogUtils.debugLogI(TAG, "Clearing DTCs", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        //bluetoothCommunicator.obdSetCtrl(ObdManager.TYPE_DTC);
    }

    public void getPids(String pids) {
        LogUtils.debugLogI(TAG, "getting pids", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        deviceManager.getPids(pids);
    }


    public void get215RtcAndMileage(){
        Log.i(TAG, "getting RTC and Mileage - ONLY if connected to 215");
        deviceManager.getRtcAndMileage();
    }

//    public void clearDTCs() {
//        Log.i(TAG, "Clearing DTCs");
//        deviceManager.obdSetCtrl(ObdManager.TYPE_DTC);
//    }

    //public void getFreeze(String tag) {
    //    Log.i(TAG, "getParameter with tag: " + tag);
    //    deviceManager.obdGetParameter(tag);
    //}

    //public void writeLoginInstruction() {
    //    String instruction = sharedPreferences.getString("loginInstruction", null);
    //    if(instruction == null) {
    //        Log.w(TAG, "No saved login instruction");
    //    } else {
    //        deviceManager.writeRawInstruction(instruction);
    //    }
    //}

    /**
     * Process result 4 data returned from OBD device for trip mileage
     * information. Create new entries for each trip. On trip end, saves
     * trip data to backend but if device gets disconnected before a
     * trip end flag is received, also save trip data to backend
     *
     * @param data The data returned from obd device for result 4
     * @see BluetoothAutoConnectService#getBluetoothState(int)
     */
    private void processResultFourData(final DataPackageInfo data) {
        if (manuallyUpdateMileage) {
            Log.i(TAG, "Currently scanning car, ignoring trips");
            return;
        }
        if (data.tripFlag.equals(ObdManager.TRIP_END_FLAG)) {
            Log.i(TAG, "Trip end flag received");
            Log.v(TAG, "Trip end package: " + data.toString());
            if (lastTripId == -1) {
                networkHelper.getLatestTrip(data.deviceId, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null && !response.equals("{}")) {
                            try {
                                lastTripId = new JSONObject(response).getInt("id");
                                sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                tripRequestQueue.add(new TripEnd(lastTripId, data.rtcTime, data.tripMileage));
                                executeTripRequests();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } else {
                tripRequestQueue.add(new TripEnd(lastTripId, data.rtcTime, data.tripMileage));
                executeTripRequests();
            }
            Car car = carAdapter.getCarByScanner(data.deviceId);
            if (car != null) {
                double newMileage = car.getTotalMileage() + Double.parseDouble(data.tripMileage) / 1000;
                car.setTotalMileage(newMileage);
                carAdapter.updateCar(car);
            }
        } else if (data.tripFlag.equals(ObdManager.TRIP_START_FLAG)) { // not needed if trips based on device trip id
            Log.i(TAG, "Trip start flag received");
            Log.v(TAG, "Trip start package: " + data.toString());
            //if(lastData != null) {
            //    sendPidDataToServer(lastData);
            //}

            //tripRequestQueue.add(new TripStart(lastDeviceTripId, data.rtcTime, data.deviceId));
            //executeTripRequests();
        }
    }

    /** makes the trip requests in order by setting the appropriate callback and then calling execute
     *  when response is received, repeat for next request
     */
    private void executeTripRequests() {
        if (!isSendingTripRequest && !tripRequestQueue.isEmpty() && NetworkHelper.isConnected(this)) {
            Log.i(TAG, "Executing trip request");
            isSendingTripRequest = true;
            final TripIndicator nextAction = tripRequestQueue.peekFirst();
            RequestCallback callback = null;
            if (nextAction instanceof TripStart) {
                if (((TripStart) nextAction).getScannerId() == null) {
                    tripRequestQueue.pop();
                }
                if (carAdapter.getCarByScanner(((TripStart) nextAction).getScannerId()) == null) {
                    isSendingTripRequest = false;
                    return;
                }
                callback = new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                lastTripId = new JSONObject(response).getInt("id");
                                sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            tripRequestQueue.pop();
                            isSendingTripRequest = false;
                            executeTripRequests();
                        } else {
                            networkHelper.getLatestTrip(((TripStart) nextAction).getScannerId(), new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    if (requestError == null) {
                                        try {
                                            lastTripId = new JSONObject(response).getInt("id");
                                            sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        tripRequestQueue.pop();
                                    } else if (requestError.getMessage().contains("no car")) {
                                        tripRequestQueue.pop();
                                    }
                                    isSendingTripRequest = false;
                                    executeTripRequests();
                                }
                            });
                        }
                    }
                };
            } else if (nextAction instanceof TripEnd) {
                callback = new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            Log.i(TAG, "trip data sent: " + ((TripEnd) nextAction).getMileage());
                        }
                        tripRequestQueue.pop();
                        isSendingTripRequest = false;
                        lastTripId = -1;
                        sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                        executeTripRequests();
                    }
                };
            }
            nextAction.execute(this, callback);
        }
    }

    /**
     * Process data package sent from device for pids. Store pids locally, once
     * we have 200 data points, send the data to the server.
     *
     * @param data The OBD data package that possibly contains obdData i.e pids
     * @see #sendPidDataToServer(String, String, String)
     * @see #extractFreezeData(DataPackageInfo)
     *
     */
    private void processPIDData(DataPackageInfo data) {
        if (data.obdData.isEmpty()) {
            Log.i(TAG, "obdData is empty");
            return;
        }

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        Car car = carAdapter.getCarByScanner(data.deviceId);

        double mileage;
        double calculatedMileage;

        if (data.tripMileage != null && !data.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(data.tripMileage) / 1000;
            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
        } else if (lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(lastData.tripMileage) / 1000;
            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
        } else {
            mileage = 0;
            calculatedMileage = 0;
        }

        pidDataObject.setMileage(mileage); // trip mileage from device
        pidDataObject.setCalculatedMileage(calculatedMileage);
        pidDataObject.setDataNumber(lastDataNum == null ? "" : lastDataNum);
        pidDataObject.setTripId(lastDeviceTripId);
        pidDataObject.setRtcTime(data.rtcTime);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        for (PIDInfo pidInfo : data.obdData) {
            //String json  = GSON.toJson(pidInfo);
            try {
                JSONObject pid = new JSONObject();
                pid.put("id", pidInfo.pidType);
                pid.put("data", pidInfo.value);
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "PID json: " + json);
        }

        pidDataObject.setPids(pids.toString());

        if (data.result == 4) {
            Log.d(TAG, "Pid array --> DB");
            Log.d(TAG, pidDataObject.getPids());
            Log.d(TAG, "rtcTime: " + pidDataObject.getRtcTime());
            Log.d(TAG, "mileage: " + pidDataObject.getMileage());
        }

        //JSONObject freezeData = extractFreezeData(data);
        //Log.d(TAG,"Freeze data --->Extract");
        //Log.d(TAG,freezeData.toString());

        if (data.result == 4) {
            Log.i(TAG, "creating PID data for result 4 - " + localPidResult4.getPidDataEntryCount());

            ArrayList<Pid> parsedPids = parsePidSet(pidDataObject);

            if (parsedPids != null) {
                for (Pid pid : parsedPids) {
                    localPidResult4.createPIDData(pid);
                }
            }
        } else if (data.result == 5) {
            Log.i(TAG, "received PID data for result 5");
            manuallyUpdateMileage = false;
            //if(pidDataObject.getTripId() == -1) {
            //    pidsWithNoTripId.add(pidDataObject);
            //} else if(pidDataObject.getMileage() >= 0 && pidDataObject.getCalculatedMileage() >= 0) {
            //    localPid.createPIDData(pidDataObject);
            //}
        }

        if(localPid.getPidDataEntryCount() >= PID_CHUNK_SIZE && localPid.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer(data.rtcTime, data.deviceId, data.tripId);
        }

        if (localPidResult4.getPidDataEntryCount() >= PID_CHUNK_SIZE && localPidResult4.getPidDataEntryCount() % PID_CHUNK_SIZE < 5) {
            sendPidDataResult4ToServer(data);
        }
    }

    /**
     * Parse result 4 pid into inividual pids
     *
     * @param result4Pid
     * @return array of parsed pids
     */
    private ArrayList<Pid> parsePidSet(Pid result4Pid) {
        final int DATA_POINT_INTERVAL = 2; // assume that all data points are 2 seconds apart
        ArrayList<Pid> parsedPids = new ArrayList<>();
        try {
            // looks like [{"id":"210D","data":"87,87,87,87,87"},{"id":"210C","data":"7AE7,7AD7,7AE7,7AD7,7AE7"}]
            JSONArray rawDataPoints = new JSONArray(result4Pid.getPids());

            // key is PID type (e.g. "210D"), value is array of values of the PID (e.g. ["87","54","32"])
            HashMap<String, String[]> dataPointMap = new HashMap<>();

            int numberOfDataPoints = 0; // number of values for each PID

            // changing values from comma separated string to array of strings and assigning to hashmap
            for (int idCount = 0; idCount < rawDataPoints.length(); idCount++) {
                JSONObject currentObject = rawDataPoints.getJSONObject(idCount);
                dataPointMap.put(currentObject.getString("id"), currentObject.getString("data").split(","));
                numberOfDataPoints = dataPointMap.get(currentObject.getString("id")).length;
            }

            ArrayList<JSONArray> obdDatas = new ArrayList<>(); // the separated pidArrays of each data point

            for (int i = 0; i < numberOfDataPoints; i++) { // for each data point, make a json array with the PIDS at that point
                JSONArray jsonArray = new JSONArray(); // pidArray of a single data point
                for (Map.Entry<String, String[]> entry : dataPointMap.entrySet()) { // put PIDs of ith data point into current pidArray
                    jsonArray.put(new JSONObject().put("id", entry.getKey()).put("data", entry.getValue()[i]));
                }
                obdDatas.add(jsonArray);
            }

            for (int i = 0; i < obdDatas.size(); i++) { // create PID object for each pidArray
                Pid pid = new Pid();
                pid.setMileage(result4Pid.getMileage());
                pid.setCalculatedMileage(result4Pid.getCalculatedMileage());
                pid.setDataNumber(result4Pid.getDataNumber());
                pid.setId(result4Pid.getId());
                pid.setTripId(result4Pid.getTripId());
                pid.setTimeStamp(result4Pid.getTimeStamp());
                pid.setPids(obdDatas.get(i).toString());
                pid.setRtcTime(String.valueOf(Integer.parseInt(result4Pid.getRtcTime()) - DATA_POINT_INTERVAL * (obdDatas.size() - i + 1)));

                parsedPids.add(pid);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return parsedPids;
    }

    private boolean isSendingPids = false;

    /**
     * Send pid data to server on 200 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param rtcTime
     * @param deviceId
     */
    private void sendPidDataToServer(final String rtcTime, final String deviceId, final String tripId) {
        if(isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID data");
        List<Pid> pidDataEntries = localPid.getAllPidDataEntries();

        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];

        try {
            for(int chunkNumber = 0 ; chunkNumber < chunks ; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }
                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("tripMileage", pidDataObject.getMileage());
                    jsonObject.put("tripIdRaw", pidDataObject.getTripId());
                    jsonObject.put("calculatedMileage", pidDataObject.getCalculatedMileage());
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(lastTripId != -1) {
            for(JSONArray pids : pidArrays) {
                if(pids.length() == 0) {
                    isSendingPids = false;
                    continue;
                }
                networkHelper.savePids(lastTripId, deviceId, pids,
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                isSendingPids = false;
                                if (requestError == null) {
                                    Log.i(TAG, "PIDS saved");
                                    localPid.deleteAllPidDataEntries();
                                } else {
                                    Log.e(TAG, "save pid error: " + requestError.getMessage());
                                    if (getDatabasePath(LocalDatabaseHelper.DATABASE_NAME).length() > 10000000L) { // delete pids if db size > 10MB
                                        localPid.deleteAllPidDataEntries();
                                        localPidResult4.deleteAllPidDataEntries();
                                    }
                                }
                            }
                        });
            }
        } else {
            isSendingPids = false;
            tripRequestQueue.add(new TripStart(Integer.parseInt(tripId), rtcTime, deviceId));
            executeTripRequests();
        }
    }

    /**
     * Send pid data on result 4 to server on 50 data points received
     *
     * @param data
     * @see #processPIDData(DataPackageInfo)
     */
    private void sendPidDataResult4ToServer(DataPackageInfo data) { // TODO: Replace all usages with sendPidDataToServer
        if(isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID result 4 data");
        List<Pid> pidDataEntries = localPidResult4.getAllPidDataEntries();

        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];

        try {
            for (int chunkNumber = 0; chunkNumber < chunks; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }
                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("tripMileage", pidDataObject.getMileage());
                    jsonObject.put("tripId", pidDataObject.getTripId());
                    jsonObject.put("calculatedMileage", pidDataObject.getCalculatedMileage());
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (lastTripId != -1) {
            for (JSONArray pids : pidArrays) {
                if (pids.length() == 0) {
                    isSendingPids = false;
                    continue;
                }
                networkHelper.savePids(lastTripId, currentDeviceId, pids,
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if (requestError == null) {
                                    Log.i(TAG, "PIDS result 4 saved");
                                    localPidResult4.deleteAllPidDataEntries();
                                } else {
                                    Log.e(TAG, "save pid result 4 error: " + requestError.getMessage());
                                    if (getDatabasePath(LocalDatabaseHelper.DATABASE_NAME).length() > 10000000L) { // delete pids if db size > 10MB
                                        localPidResult4.deleteAllPidDataEntries();
                                    }
                                }
                                isSendingPids = false;
                            }
                        });
            }
        } else {
            isSendingPids = false;
            tripRequestQueue.add(new TripStart(lastDeviceTripId, data.rtcTime, currentDeviceId));
            executeTripRequests();
        }
    }

    /**
     * Extract freeze data from data package sent from device
     *
     * @param data
     * @return JSONObject
     */
    private JSONObject extractFreezeData(DataPackageInfo data) {
        JSONObject jsonObject = new JSONObject();
        JSONArray freezeData = new JSONArray();

        if (data.freezeData.isEmpty()) {
            Log.i(TAG, "No freeze Data");
            return jsonObject;
        }

        Car car = new LocalCarAdapter(getApplicationContext()).getCarByScanner(data.deviceId);

        double mileage;

        if (lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(lastData.tripMileage) / 1000;
        } else if (data.tripMileage == null || data.tripMileage.isEmpty()) {
            mileage = 0.0;
        } else {
            mileage = Double.parseDouble(data.tripMileage) / 1000;
        }

        mileage += car == null ? 0 : car.getTotalMileage();

        try {
            jsonObject.put("dataNum", lastDataNum == null ? "" : lastDataNum);
            jsonObject.put("rtcTime", data.rtcTime);
            jsonObject.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            jsonObject.put("mileage", mileage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (PIDInfo pidInfo : data.freezeData) {
            Log.v("Freeze Data", "pidType: " + pidInfo.pidType + ", value: " + pidInfo.value);
            try {
                JSONObject dataObject = new JSONObject();
                dataObject.put("pidType", pidInfo.pidType).put("value", pidInfo.value);
                freezeData.put(dataObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            jsonObject.put("pids", freezeData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public int getLastTripId() {
        return lastTripId;
    }

    // hardcoded linked list that is in the order of priority
    private void initPidPriorityList() {
        PID_PRIORITY.add("210C");
        PID_PRIORITY.add("210D");
        PID_PRIORITY.add("2106");
        PID_PRIORITY.add("2107");
        PID_PRIORITY.add("2110");
        PID_PRIORITY.add("2124");
        PID_PRIORITY.add("2105");
        PID_PRIORITY.add("210E");
        PID_PRIORITY.add("210F");
        PID_PRIORITY.add("2142");
        PID_PRIORITY.add("210A");
        PID_PRIORITY.add("210B");
        PID_PRIORITY.add("2104");
        PID_PRIORITY.add("2111");
        PID_PRIORITY.add("212C");
        PID_PRIORITY.add("212D");
        PID_PRIORITY.add("215C");
        PID_PRIORITY.add("2103");
        PID_PRIORITY.add("212E");
    }

    public String getConnectedDeviceName() {
//        return null;  //bluetoothCommunicator.getConnectedDeviceName();
        return deviceManager.getConnectedDeviceName();
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

    /**
     * BroadcastReceiver made specifically for BluetoothAutoConnectService
     */
    private final class BluetoothServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "Bond state changed: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                    startBluetoothSearch(5);  // start search after pairing in case it disconnects after pair
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i(TAG, "Bluetooth adapter state changed: " + state);
                deviceManager.bluetoothStateChanged(state);
                if(state == BluetoothAdapter.STATE_OFF) {
                    deviceManager.close();
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(notifID);
                } else if(state == BluetoothAdapter.STATE_ON && BluetoothAdapter.getDefaultAdapter() != null) {
                    if(deviceManager != null) {
                        deviceManager.close();
                    }

                    deviceManager = new BluetoothDeviceManager(BluetoothAutoConnectService.this);

                    deviceManager.setBluetoothDataListener(BluetoothAutoConnectService.this);
                    if (BluetoothAdapter.getDefaultAdapter()!=null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startBluetoothSearch(6); // start search when turning bluetooth on
                    }
                }
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) { // internet connectivity listener
                if (NetworkHelper.isConnected(BluetoothAutoConnectService.this)) {
                    Log.i(TAG, "Sending stored PIDS and DTCS");
                    executeTripRequests();
                    if (lastData != null) {
                        sendPidDataResult4ToServer(lastData);
                    }
                    for (final Dtc dtc : dtcsToSend) {
                        networkHelper.addNewDtc(dtc.getCarId(), dtc.getMileage(),
                                dtc.getRtcTime(), dtc.getDtcCode(), dtc.isPending(),
                                new RequestCallback() {
                                    @Override
                                    public void done(String response, RequestError requestError) {
                                        Log.i(TAG, "DTC added: " + dtc);

                                        //INCLUDE THIS IN USE CASE IN LATER REFACTOR
                                        //Send notification that dtcs have been updated
                                        // after last one has been sent
                                        if (dtcsToSend.indexOf(dtc) == dtcsToSend.size()-1){
                                            notifyEventBus(new EventTypeImpl(EventType
                                                    .EVENT_DTC_NEW));
                                        }
                                    }
                                });
                    }
                }
            }
        }
    }

    //Null means not connected
    public Boolean isConnectedTo215(){
        if (deviceManager.getConnectionState() == BluetoothCommunicator.CONNECTED)
            return deviceManager.isConnectedTo215();
        else
            return null;
    }

}

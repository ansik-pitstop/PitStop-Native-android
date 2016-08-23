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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothClassicComm;
import com.castel.obd.bluetooth.BluetoothLeComm;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.ObdDataUtil;
import com.google.gson.Gson;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.Dtc;
import com.pitstop.models.Pid;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalPidAdapter;
import com.pitstop.database.LocalPidResult4Adapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.R;
import com.pitstop.models.TripEnd;
import com.pitstop.models.TripIndicator;
import com.pitstop.models.TripStart;
import com.pitstop.utils.NetworkHelper;

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
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener {

    private static Gson GSON = new Gson();

    private static String SYNCED_DEVICE = "SYNCED_DEVICE";
    private static String DEVICE_ID = "deviceId";
    private static String DEVICE_IDS = "deviceIds";

    private final IBinder mBinder = new BluetoothBinder();
    private IBluetoothCommunicator bluetoothCommunicator;
    private ObdManager.IBluetoothDataListener callbacks;

    private boolean isGettingVin = false;
    private boolean gettingPIDs = false;
    private boolean gettingPID = false;
    private boolean deviceConnState = false;

    public static int notifID = 1360119;
    private String currentDeviceId = null;
    private DataPackageInfo lastData = null;

    private int counter = 1;
    private int status5counter = 0;

    private NetworkHelper networkHelper;

    private String[] pids = new String[0];
    private int pidI = 0;

    private LocalPidAdapter localPid;
    private LocalPidResult4Adapter localPidResult4;
    private int PID_CHUNK_SIZE = 100;

    private String lastDataNum = "";

    private SharedPreferences sharedPreferences;
    private LocalCarAdapter localCarAdapter;

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

    private static String TAG = "BtAutoConnectDebug";

    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();
    private String supportedPids = "";
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        networkHelper = new NetworkHelper(getApplicationContext());

        if(BluetoothAdapter.getDefaultAdapter() != null) {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothCommunicator = new BluetoothLeComm(this);  // TODO: BLE
            } else {
                bluetoothCommunicator = new BluetoothClassicComm(this);
            }

            bluetoothCommunicator.setBluetoothDataListener(this);
            if (BluetoothAdapter.getDefaultAdapter()!=null
                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                startBluetoothSearch(3);  // start search when service starts
            }
        }
        localPid = new LocalPidAdapter(this);
        localPidResult4 = new LocalPidResult4Adapter(this);
        localCarAdapter = new LocalCarAdapter(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        lastTripId = sharedPreferences.getInt(pfTripId, -1);
        lastDeviceTripId = sharedPreferences.getInt(pfDeviceTripId, -1);
        lastTripMileage = sharedPreferences.getInt(pfTripMileage, 0);

        initPidPriorityList();

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);

        Runnable runnable = new Runnable() { // start background search
            @Override
            public void run() { // this is for auto connect for bluetooth classic
                if(BluetoothAdapter.getDefaultAdapter().isEnabled() &&
                        bluetoothCommunicator.getState() == IBluetoothCommunicator.DISCONNECTED) {
                    Log.d(TAG, "Running periodic scan");
                    startBluetoothSearch(4); // periodic scan
                }

                handler.postDelayed(this, 120000);
            }
        };

        handler.postDelayed(runnable, 15000);
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

        super.onDestroy();
        bluetoothCommunicator.close();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void getBluetoothState(int state) {
        if(state==IBluetoothCommunicator.CONNECTED) {
            //show a custom notification
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_push);

            Car connectedCar = localCarAdapter.getCarByScanner(getCurrentDeviceId());

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
        } else {
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

            if(currentDeviceId != null && lastData != null && !localPidResult4.getAllPidDataEntries().isEmpty()) {
                sendPidDataResult4ToServer(lastData);
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifID);

        }

        if (callbacks != null) {
            Log.i(TAG, "Calling service callbacks to getBluetooth State - auto connect service");
            callbacks.getBluetoothState(state);
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if(callbacks != null) {
            Log.i(TAG,"Setting ctrl response on service callbacks - auto-connect service");
            callbacks.setCtrlResponse(responsePackageInfo);
        }
    }


    /**
     * @param responsePackageInfo
     *          The response from device for a parameter that
     *          was successfully set.
     * If device time was set, save the id of the device.
     * */
    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        if((responsePackageInfo.type+responsePackageInfo.value)
                .equals(ObdManager.RTC_TAG)) {
            // Once device time is reset, store deviceId
            currentDeviceId = responsePackageInfo.deviceId;
            saveSyncedDevice(responsePackageInfo.deviceId);
        }

        if(callbacks!=null) {
            Log.i(TAG, "Setting parameter response on service callbacks - auto-connect service");
            callbacks.setParameterResponse(responsePackageInfo);
        }
    }


    /**
     * @param parameterPackageInfo
     *          The response sent from device upon querying the
     *          obd device for a specific tag or list of tags
     * If response is for device time, check if returned value for
     * is within 1 year. If device time is not within range then sync.
     * @see #syncObdDevice()
     * */
    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

        Log.i(TAG, "getParameterData(): "+parameterPackageInfo.value.get(0).value);

        if(gettingPID){
            Log.i(TAG,"Getting parameter data- auto-connect service");
            pids = parameterPackageInfo.value.get(0).value.split(","); // pids returned separated by commas
            HashSet<String> supportedPidsSet = new HashSet<>(Arrays.asList(pids));
            StringBuilder sb = new StringBuilder();
            int pidCount = 0;
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
            setParam(ObdManager.FIXED_UPLOAD_TAG, "01;01;01;10;2;" + supportedPids);

            //setFixedUpload();

            pidI = 0;
            sendForPIDS();
            gettingPID=false;
        }

        if(isGettingVin) {
            isGettingVin = false;
            //if(parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {
            //    Log.i(TAG, "Device time returned: "+parameterPackageInfo.value.get(0).value);
            //    long moreThanOneYear = 32000000;
            //    long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
            //    long currentTime = System.currentTimeMillis()/1000;
            //    long diff = currentTime - deviceTime;
            //    if(diff > moreThanOneYear) {
            //        syncObdDevice();
            //    } else {
            //        saveSyncedDevice(parameterPackageInfo.deviceId);
            //        getVinFromCar();
            //        isGettingVin = false;
            //    }
            //}
        }

        if(callbacks != null) {
            Log.i(TAG, "getting parameter data on service Callbacks - auto-connect service");
            callbacks.getParameterData(parameterPackageInfo);
        }
    }


    /**
     * result=4 --> trip data uploaded by terminal
     * result=5 --> PID data uploaded by terminal
     * result=6 --> OBD monitor data uploaded from the terminal after the monitor command
     *
     * @see #processPIDData(DataPackageInfo)
     * @see #processResultFourData(DataPackageInfo)
     */
    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

        Log.v(TAG, dataPackageInfo.toString());



        if(dataPackageInfo.dataNumber != null) {
            lastDataNum = dataPackageInfo.dataNumber;
        }

        deviceConnState = true;
        if(dataPackageInfo.deviceId != null && !dataPackageInfo.deviceId.isEmpty()) {
            currentDeviceId = dataPackageInfo.deviceId;
        }

        if(dataPackageInfo.rtcTime != null && !dataPackageInfo.rtcTime.isEmpty()) {
            lastData = dataPackageInfo;
        }

        // if trip id is different, start a new trip
        if(dataPackageInfo.tripId != null && !dataPackageInfo.tripId.isEmpty()
                && localCarAdapter.getCarByScanner(dataPackageInfo.deviceId) != null) {
            int newTripId = Integer.parseInt(dataPackageInfo.tripId);
            if(newTripId != lastDeviceTripId) {
                lastDeviceTripId = newTripId;
                sharedPreferences.edit().putInt(pfDeviceTripId, newTripId).apply();

                if(lastData != null) {
                    sendPidDataResult4ToServer(lastData);
                }
                tripRequestQueue.add(new TripStart(lastDeviceTripId, dataPackageInfo.rtcTime, dataPackageInfo.deviceId));
                executeTripRequests();
            }
        }

        if(dataPackageInfo.result == 4) {
            processResultFourData(dataPackageInfo);
            processPIDData(dataPackageInfo);
        }

        if(dataPackageInfo.result == 5) {
            processPIDData(dataPackageInfo);
        }

        Log.d(TAG, "getting io data - auto-connect service");

        if(dataPackageInfo.result == 6) { //save dtcs
            saveDtcs(dataPackageInfo, false, dataPackageInfo.deviceId);
        } else if (dataPackageInfo.tripFlag != null && dataPackageInfo.tripFlag.equals("5")) {
            saveDtcs(dataPackageInfo, false, dataPackageInfo.deviceId);
        } else if (dataPackageInfo.tripFlag != null && dataPackageInfo.tripFlag.equals("6")) {
            saveDtcs(dataPackageInfo, true, dataPackageInfo.deviceId);
        }

        counter ++;
        //keep looking for pids until all pids are recieved
        if(pidI!=pids.length&&dataPackageInfo.result!=5){
            sendForPIDS();
        }
        //because theres a lot of status 5, keep looking
        if(dataPackageInfo.result==5){
            status5counter++;
        }

        if (callbacks != null) {
            Log.d(TAG, "calling service callbacks for getIOdata - auto-connect service");
            callbacks.getIOData(dataPackageInfo);
        }

        if(counter==50){
            if(!isGettingVin) {
                getPIDs();
            }
        }
        if(counter%500==0){
            getDTCs();
        }
        if(counter%600==0){
            getPendingDTCs();
        }
        if(counter==1000){
            counter = 1;
        }

    }

    private void saveDtcs(final DataPackageInfo dataPackageInfo, final boolean isPendingDtc, final String deviceId) {
        Log.i(TAG, "save DTCs - auto-connect service");
        String dtcs = "";
        final ArrayList<String> dtcArr = new ArrayList<>();
        if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
            String[] DTCs = dataPackageInfo.dtcData.split(",");
            for(String dtc : DTCs) {
                String parsedDtc = ObdDataUtil.parseDTCs(dtc);
                dtcs+= parsedDtc+",";
                dtcArr.add(parsedDtc);
            }
        }

        Log.i(TAG, "DTCs found: " + dtcs);

        Car car = localCarAdapter.getCarByScanner(dataPackageInfo.deviceId);

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

                                for (final String dtc : dtcArr) {
                                    if (!dtcNames.contains(dtc)) {
                                        networkHelper.addNewDtc(car.getId(), car.getTotalMileage(),
                                                dataPackageInfo.rtcTime, dtc, isPendingDtc, dataPackageInfo.freezeData,
                                                new RequestCallback() {
                                                    @Override
                                                    public void done(String response, RequestError requestError) {
                                                        Log.i(TAG, "DTC added: " + dtc);
                                                    }
                                                });
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } else if(car != null) {
            Log.i(TAG, "Saving dtcs offline");
            for (final String dtc : dtcArr) {
                dtcsToSend.add(new Dtc(car.getId(), car.getTotalMileage(), dataPackageInfo.rtcTime,
                        dtc, isPendingDtc, dataPackageInfo.freezeData));
            }
        }

        if (callbacks != null)
            callbacks.getIOData(dataPackageInfo);
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            Log.i(TAG,"Device login: "+loginPackageInfo.deviceId);
            Log.i(TAG,"Device result: "+loginPackageInfo.result);
            Log.i(TAG,"Device flag: "+loginPackageInfo.flag);
            Log.i(TAG,"Device instruction: "+loginPackageInfo.instruction);

            sharedPreferences.edit().putString("loginInstruction", loginPackageInfo.instruction).apply();

            currentDeviceId = loginPackageInfo.deviceId;
            bluetoothCommunicator.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);
        } else if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }

        if(callbacks != null) {
            callbacks.deviceLogin(loginPackageInfo);
        }

    }

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void setCallbacks(ObdManager.IBluetoothDataListener callBacks) {
        this.callbacks = callBacks;
    }

    public void startBluetoothSearch(int... source) {
        Log.d(TAG, "startBluetoothSearch() " + ((source != null && source.length > 0) ? source[0] : ""));
        bluetoothCommunicator.startScan();
    }

    /**
     * @return The connection state of the obd device to the car.
     * If device is sending data packages with result greater than
     * 3, then device is connected
     * @see #getIOData(DataPackageInfo)
     */
    public boolean isCommunicatingWithDevice() {
        return deviceConnState;
    }

    public boolean hasDiscoveredServices() {
        return bluetoothCommunicator.hasDiscoveredServices();
    }


    /**
     * @return The device id of the currently connected obd device
     * */
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }


    /**
     * Gets the Car's VIN. Check if obd device is synced. If synced,
     * send command to device to retrieve vin info.
     * @see #getObdDeviceTime()
     * @see #getParameterData(ParameterPackageInfo)
     */
    public void getCarVIN() {
        Log.i(TAG, "getCarVin");
        isGettingVin = true;
        getObdDeviceTime();
    }

    /**
     * Send command to obd device to retrieve vin from the currently
     * connected car.
     * @see #getParameterData(ParameterPackageInfo) for info returned
     * on the vin query.
     * */
    public void getVinFromCar() {
        Log.i(TAG, "Calling getCarVIN from Bluetooth auto-connect");
        bluetoothCommunicator.obdGetParameter(ObdManager.VIN_TAG);
    }


    /**
     * Send command to obd device to retrieve the current device time.
     * @see #getParameterData(ParameterPackageInfo) for device time returned
     * by obd device.
     */
    public void getObdDeviceTime() {
        Log.i(TAG, "Getting device time");
        bluetoothCommunicator.obdGetParameter(ObdManager.RTC_TAG);
    }


    /**
     * Sync obd device time with current mobile device time.
     * On successfully syncing device,  #setParameter() gets called
     * @see #setParameterResponse(ResponsePackageInfo)
     * */
    public void syncObdDevice() {
        Log.i(TAG,"Resetting RTC time - BluetoothAutoConn");

        long systemTime = System.currentTimeMillis();
        bluetoothCommunicator
                .obdSetParameter(ObdManager.RTC_TAG, String.valueOf(systemTime / 1000));
    }

    public void resetObdDeviceTime() {
        Log.i(TAG,"Setting RTC time to 200x - BluetoothAutoConn");

        bluetoothCommunicator
                .obdSetParameter(ObdManager.RTC_TAG, String.valueOf(1088804101));
    }

    public void setFixedUpload() { // to make result 4 pids send every 10 seconds
        Log.i(TAG, "Setting fixed upload parameters");
        bluetoothCommunicator.obdSetParameter(ObdManager.FIXED_UPLOAD_TAG,
                "01;01;01;10;2;2105,2106,2107,210c,210d,210e,210f,2110,2124,2142");
    }

    public void setParam(String tag, String values) {
        Log.i(TAG,"Setting param with tag: " + tag + ", values: " + values);

        bluetoothCommunicator.obdSetParameter(tag, values);
    }

    public void resetDeviceToFactory() {
        Log.i(TAG, "Resetting device to factory settings");

        bluetoothCommunicator.obdSetCtrl(4);
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
     * @param deviceId
     *          The device id of the currently connected obd device
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
        return sharedPreferences.getString(DEVICE_ID,null);
    }

    public int getState() {
        return bluetoothCommunicator.getState();
    }


    public void getPIDs(){ // supported pids
        Log.i(TAG,"getting PIDs - auto-connect service");
        bluetoothCommunicator.obdGetParameter(ObdManager.PID_TAG);
        gettingPID = true;
    }

    public void getDTCs() {
        Log.i(TAG, "calling getting DTCs - auto-connect service");
        bluetoothCommunicator.obdSetMonitor(ObdManager.TYPE_DTC, "");
    }

    public void getPendingDTCs() {
        Log.i(TAG, "Getting pending DTCs");
        bluetoothCommunicator.obdSetMonitor(ObdManager.TYPE_PENDING_DTC, "");
    }

    public void clearDTCs() {
        Log.i(TAG, "Clearing DTCs");
        bluetoothCommunicator.obdSetCtrl(ObdManager.TYPE_DTC);
    }

    public void getFreeze(String tag) {
        Log.i(TAG, "getParameter with tag: " + tag);
        bluetoothCommunicator.obdGetParameter(tag);
    }

    public void writeLoginInstruction() {
        String instruction = sharedPreferences.getString("loginInstruction", null);
        if(instruction == null) {
            Log.w(TAG, "No saved login instruction");
        } else {
            bluetoothCommunicator.writeRawInstruction(instruction);
        }
    }

    public void initialize() {
        bluetoothCommunicator.initDevice();
    }

    private void sendForPIDS(){
        Log.d(TAG, "Sending for PIDS - auto-connect service");
        gettingPIDs = true;
        String pid="";
        while(pidI!=pids.length){
            pid+=pids[pidI]+",";
            if  ((pidI+1)%9 ==0){
                bluetoothCommunicator
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
                pidI++;
                return;
            }else if ((pidI+1)==pids.length){
                bluetoothCommunicator
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
            }
            pidI++;
        }
    }

    /**
     * Process result 4 data returned from OBD device for trip mileage
     * information. Create new entries for each trip. On trip end, saves
     * trip data to backend but if device gets disconnected before a
     * trip end flag is received, also save trip data to backend
     * @see BluetoothAutoConnectService#getBluetoothState(int)
     * @param data
     *      The data returned from obd device for result 4
     */
    private void processResultFourData(final DataPackageInfo data) {
        if(manuallyUpdateMileage) {
            Log.i(TAG, "Currently scanning car, ignoring trips");
            return;
        }
        if(data.tripFlag.equals(ObdManager.TRIP_END_FLAG)) {
            Log.i(TAG, "Trip end flag received");
            Log.v(TAG, "Trip end package: " + data.toString());
            if(lastTripId == -1) {
                networkHelper.getLatestTrip(data.deviceId, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null && !response.equals("{}")) {
                            try {
                                lastTripId = new JSONObject(response).getInt("id");
                                sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                tripRequestQueue.add(new TripEnd(lastTripId, data.rtcTime, data.tripMileage));
                                executeTripRequests();
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } else {
                tripRequestQueue.add(new TripEnd(lastTripId, data.rtcTime, data.tripMileage));
                executeTripRequests();
            }
            Car car = localCarAdapter.getCarByScanner(data.deviceId);
            if(car != null) {
                double newMileage = car.getTotalMileage() + Double.parseDouble(data.tripMileage) / 1000;
                car.setTotalMileage(newMileage);
                localCarAdapter.updateCar(car);
            }
        } else if(data.tripFlag.equals(ObdManager.TRIP_START_FLAG)) { // not needed if trips based on device trip id
            Log.i(TAG, "Trip start flag received");
            Log.v(TAG, "Trip start package: " + data.toString());

            //if(lastData != null) {
            //    sendPidDataToServer(lastData);
            //}
//
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
                if(((TripStart) nextAction).getScannerId() == null) {
                    tripRequestQueue.pop();
                }
                if(localCarAdapter.getCarByScanner(((TripStart) nextAction).getScannerId()) == null) {
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
                                    } else if(requestError.getMessage().contains("no car")) {
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
                            Toast.makeText(BluetoothAutoConnectService.this, "Trip data saved", Toast.LENGTH_LONG).show();
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
     * we have 100 data points, send the data to the server.
     * @see #sendPidDataToServer(DataPackageInfo)
     * @see #extractFreezeData(DataPackageInfo)
     *
     * @param data
     *      The OBD data package that possibly contains obdData i.e pids
     *
     */
    private void processPIDData(DataPackageInfo data) {
        if (data.obdData.isEmpty()) {
            Log.i(TAG, "obdData is empty");
            return;
        }

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        Car car = localCarAdapter.getCarByScanner(data.deviceId);

        double mileage;
        double calculatedMileage;

        if(data.tripMileage != null && !data.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(data.tripMileage) / 1000;
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
        pidDataObject.setTripId(lastDeviceTripId);
        pidDataObject.setRtcTime(data.rtcTime);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        for(PIDInfo pidInfo : data.obdData) {
            //String json  = GSON.toJson(pidInfo);
            try {
                JSONObject pid = new JSONObject();
                pid.put("id",pidInfo.pidType);
                pid.put("data",pidInfo.value);
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "PID json: " + json);
        }

        pidDataObject.setPids(pids.toString());

        if(data.result == 4) {
            Log.i(TAG, "Pid array --> DB");
            Log.i(TAG, pidDataObject.getPids());
            Log.i(TAG, "rtcTime: " + pidDataObject.getRtcTime());
            Log.i(TAG, "mileage: " + pidDataObject.getMileage());
        }

        //JSONObject freezeData = extractFreezeData(data);
        //Log.d(TAG,"Freeze data --->Extract");
        //Log.d(TAG,freezeData.toString());

        if(data.result == 4) {
            Log.i(TAG, "creating PID data for result 4 - " + localPidResult4.getPidDataEntryCount());

            ArrayList<Pid> parsedPids = parsePidSet(pidDataObject);

            if(parsedPids != null) {
                for (Pid pid : parsedPids) {
                    localPidResult4.createPIDData(pid);
                }
            }
        } else if(data.result == 5) {
            Log.i(TAG, "received PID data for result 5");
            manuallyUpdateMileage = false;
            //if(pidDataObject.getTripId() == -1) {
            //    pidsWithNoTripId.add(pidDataObject);
            //} else if(pidDataObject.getMileage() >= 0 && pidDataObject.getCalculatedMileage() >= 0) {
            //    localPid.createPIDData(pidDataObject);
            //}
        }

        if(localPid.getPidDataEntryCount() >= PID_CHUNK_SIZE && localPid.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer(data);
        }

        if(localPidResult4.getPidDataEntryCount() >= PID_CHUNK_SIZE && localPidResult4.getPidDataEntryCount() % PID_CHUNK_SIZE < 5) {
            sendPidDataResult4ToServer(data);
        }
    }

    /**
     * Parse result 4 pid into inividual pids
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
            HashMap<String, String[] > dataPointMap = new HashMap<>();

            int numberOfDataPoints = 0; // number of values for each PID

            // changing values from comma separated string to array of strings and assigning to hashmap
            for(int idCount = 0 ; idCount < rawDataPoints.length() ; idCount++) {
                JSONObject currentObject = rawDataPoints.getJSONObject(idCount);
                dataPointMap.put(currentObject.getString("id"), currentObject.getString("data").split(","));
                numberOfDataPoints = dataPointMap.get(currentObject.getString("id")).length;
            }

            ArrayList<JSONArray> obdDatas = new ArrayList<>(); // the separated pidArrays of each data point

            for(int i = 0 ; i < numberOfDataPoints ; i++) { // for each data point, make a json array with the PIDS at that point
                JSONArray jsonArray = new JSONArray(); // pidArray of a single data point
                for (Map.Entry<String, String[]> entry : dataPointMap.entrySet()) { // put PIDs of ith data point into current pidArray
                    jsonArray.put(new JSONObject().put("id", entry.getKey()).put("data", entry.getValue()[i]));
                }
                obdDatas.add(jsonArray);
            }

            for(int i = 0 ; i < obdDatas.size() ; i++) { // create PID object for each pidArray
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
     * Send pid data to server on 100 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param data
     */
    private void sendPidDataToServer(final DataPackageInfo data) {
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
                    if(chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
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
                    continue;
                }
                networkHelper.savePids(lastTripId, data.deviceId, pids,
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                isSendingPids = false;
                                if (requestError == null) {
                                    Log.i(TAG, "PIDS saved");
                                    localPid.deleteAllPidDataEntries();
                                } else {
                                    Log.e(TAG, "save pid error: " + requestError.getMessage());
                                    if(getDatabasePath(LocalDatabaseHelper.DATABASE_NAME).length() > 10000000L) { // delete pids if db size > 10MB
                                        localPid.deleteAllPidDataEntries();
                                        localPidResult4.deleteAllPidDataEntries();
                                    }
                                }
                            }
                        });
            }
        } else {
            isSendingPids = false;
            tripRequestQueue.add(new TripStart(lastDeviceTripId, data.rtcTime, data.deviceId));
            executeTripRequests();
        }
    }

    /**
     * Send pid data on result 4 to server on 50 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param data
     */
    private void sendPidDataResult4ToServer(DataPackageInfo data) {
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
            for(int chunkNumber = 0 ; chunkNumber < chunks ; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if(chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
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

        if(lastTripId != -1) {
            for(JSONArray pids : pidArrays) {
                if(pids.length() == 0) {
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
                                    if(getDatabasePath(LocalDatabaseHelper.DATABASE_NAME).length() > 10000000L) { // delete pids if db size > 10MB
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
     * @see #sendPidDataToServer(DataPackageInfo)
     * @param data
     * @return JSONObject
     */
    private JSONObject extractFreezeData(DataPackageInfo data) {
        JSONObject jsonObject = new JSONObject();
        JSONArray freezeData = new JSONArray();

        if(data.freezeData.isEmpty()) {
            Log.i(TAG,"No freeze Data");
            return jsonObject;
        }

        Car car = new LocalCarAdapter(getApplicationContext()).getCarByScanner(data.deviceId);

        double mileage;

        if(lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(lastData.tripMileage)/1000;
        } else if(data.tripMileage == null || data.tripMileage.isEmpty()) {
            mileage = 0.0;
        } else {
            mileage = Double.parseDouble(data.tripMileage) / 1000;
        }

        mileage += car == null ? 0 : car.getTotalMileage();

        try {
            jsonObject.put("dataNum", lastDataNum == null ? "" : lastDataNum);
            jsonObject.put("rtcTime", data.rtcTime);
            jsonObject.put("timestamp", String.valueOf(System.currentTimeMillis()/1000));
            jsonObject.put("mileage", mileage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(PIDInfo pidInfo : data.freezeData) {
            Log.v("Freeze Data","pidType: " + pidInfo.pidType + ", value: " + pidInfo.value);
            try {
                JSONObject dataObject = new JSONObject();
                dataObject.put("pidType", pidInfo.pidType).put("value", pidInfo.value);
                freezeData.put(dataObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            jsonObject.put("pids",freezeData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() { // for both bluetooth and internet
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {  // device pairing listener
                Log.i(TAG, "Bond state changed: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                    startBluetoothSearch(5);  // start search after pairing in case it disconnects after pair
                }
            } else if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {  // bluetooth adapter state listener
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i(TAG, "Bluetooth adapter state changed: " + state);
                bluetoothCommunicator.bluetoothStateChanged(state);
                if(state == BluetoothAdapter.STATE_OFF) {
                    bluetoothCommunicator.close();
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(notifID);
                } else if(state == BluetoothAdapter.STATE_ON && BluetoothAdapter.getDefaultAdapter() != null) {
                    if(bluetoothCommunicator != null) {
                        bluetoothCommunicator.close();
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        bluetoothCommunicator = new BluetoothLeComm(BluetoothAutoConnectService.this); // TODO: BLE
                    } else {
                        bluetoothCommunicator = new BluetoothClassicComm(BluetoothAutoConnectService.this);
                    }

                    bluetoothCommunicator.setBluetoothDataListener(BluetoothAutoConnectService.this);
                    if (BluetoothAdapter.getDefaultAdapter()!=null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startBluetoothSearch(6); // start search when turning bluetooth on
                    }
                }
            } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {  // internet connectivity listener
                if(NetworkHelper.isConnected(BluetoothAutoConnectService.this)) {
                    Log.i(TAG, "Sending stored PIDS and DTCS");
                    executeTripRequests();
                    if(lastData != null) {
                        sendPidDataResult4ToServer(lastData);
                    }
                    for(final Dtc dtc : dtcsToSend) {
                        networkHelper.addNewDtc(dtc.getCarId(), dtc.getMileage(),
                                dtc.getRtcTime(), dtc.getDtcCode(), dtc.isPending(), dtc.getFreezeData(),
                                new RequestCallback() {
                                    @Override
                                    public void done(String response, RequestError requestError) {
                                        Log.i(TAG, "DTC added: " + dtc);
                                    }
                                });
                    }
                }
            }
        }
    };

    private Handler handler = new Handler(); // for periodic bluetooth scans

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
}

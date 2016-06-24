package com.pitstop.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
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
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.DTOs.Pid;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalPidAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalPidResult4Adapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Responses;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener {

    private static Gson GSON = new Gson();

    private static String SYNCED_DEVICE = "SYNCED_DEVICE";
    private static String DEVICE_ID = "deviceId";

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

    String[] pids = new String[0];
    int pidI = 0;

    private LocalPidAdapter localPid;
    private LocalPidResult4Adapter localPidResult4;

    private String lastDataNum = "";

    private static String TAG = "BtAutoConnectDebug";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        networkHelper = new NetworkHelper(getApplicationContext());

        if(BluetoothAdapter.getDefaultAdapter() != null) {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothCommunicator = new BluetoothLeComm(this);
            } else {
                bluetoothCommunicator = new BluetoothClassicComm(this);
            }

            bluetoothCommunicator.setBluetoothDataListener(this);
            if (BluetoothAdapter.getDefaultAdapter()!=null
                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                bluetoothCommunicator.startScan();
            }
        }
        localPid = new LocalPidAdapter(this);
        localPidResult4 = new LocalPidResult4Adapter(this);

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, intentFilter);
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
            unregisterReceiver(bluetoothReceiver);
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
            BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            List<BluetoothDevice> devices = new LinkedList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            }
            boolean deviceConnected = false;
            for (BluetoothDevice device : devices) {
                //if device has name IDD-212
                if (device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                    deviceConnected = true;
                }
            }
            //show a custom notification
            if (deviceConnected) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_push);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                                .setLargeIcon(icon)
                                .setColor(getResources().getColor(R.color.highlight))
                                .setContentTitle("Car is Connected")
                                .setContentText("Click here to check out more");
                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.putExtra(MainActivity.FROM_NOTIF, true);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
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
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(notifID);
            }

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

            if(currentDeviceId != null && lastData != null) {
                sendPidDataToServer(lastData);
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
            pids  =parameterPackageInfo.value.get(0).value.split(",");
            pidI = 0;
            sendForPIDS();
            gettingPID=false;
        }

        if(isGettingVin) {
            if(parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {
                Log.i(TAG, "Device time returned: "+parameterPackageInfo.value.get(0).value);
                long moreThanOneYear = 32000000;
                long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
                long currentTime = System.currentTimeMillis()/1000;
                long diff = currentTime - deviceTime;
                if(diff > moreThanOneYear) {
                    syncObdDevice();
                } else {
                    saveSyncedDevice(parameterPackageInfo.deviceId);
                    getVinFromCar();
                    isGettingVin = false;
                }
            }
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

        if(dataPackageInfo.dataNumber != null) {
            lastDataNum = dataPackageInfo.dataNumber;
        }

        deviceConnState = true;
        currentDeviceId = dataPackageInfo.deviceId;
        if(dataPackageInfo.tripMileage != null) {
            lastData = dataPackageInfo;
        }
        processPIDData(dataPackageInfo);

        if(dataPackageInfo.result == 4) {
            processResultFourData(dataPackageInfo);
        }

        if(dataPackageInfo.result == 5) {
            //processResultFiveData(dataPackageInfo);
        }

        Log.i(TAG, "getting io data - auto-connect service");

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

        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Responses response = new Responses();

        if(dataPackageInfo.result==1||dataPackageInfo.result==3||dataPackageInfo.result==4||
                dataPackageInfo.result==6||status5counter%20==1) {
            if (status5counter % 20 == 1)
                status5counter = 1;
            response.setValue("result", "" + dataPackageInfo.result);
            response.setValue("deviceId", dataPackageInfo.deviceId);
            response.setValue("tripId", dataPackageInfo.tripId);
            response.setValue("dataNumber", dataPackageInfo.dataNumber);
            response.setValue("tripFlag", dataPackageInfo.tripFlag);
            response.setValue("rtcTime", dataPackageInfo.rtcTime);
            response.setValue("protocolType", dataPackageInfo.protocolType);
            response.setValue("tripMileage", dataPackageInfo.tripMileage);
            response.setValue("tripfuel", dataPackageInfo.tripfuel);
            response.setValue("vState", dataPackageInfo.vState);

            String OBD = "{";
            boolean recordedOnce = false;
            for (PIDInfo i : dataPackageInfo.obdData) {
                OBD += (recordedOnce ? ";'" : "'") + i.pidType + "':" + i.value;
                recordedOnce = true;
            }
            OBD += "}";
            response.setValue("OBD", OBD);
            if (pidI<=pids.length&&gettingPIDs&&dataPackageInfo.obdData.size()>0) {
                if(pidI==pids.length){
                    gettingPIDs= false;
                }
                JSONObject Freeze = new JSONObject();
                JSONArray arrayOfPids = new JSONArray();
                try {
                    //freeze data will need to be stored ina  different format (PIDS)
                    Freeze.put("time", dataPackageInfo.rtcTime);
                    JSONObject individual = new JSONObject();
                    for (PIDInfo i : dataPackageInfo.obdData) {
                        individual.put("id", i.pidType);
                        individual.put("data", i.value);
                        arrayOfPids.put(individual);
                    }
                    Freeze.put("pids", arrayOfPids);
                    response.setValue("Freeze", Freeze.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            response.setValue("supportPid", dataPackageInfo.surportPid);
            response.setValue("dtcData", dataPackageInfo.dtcData);
            Log.i(TAG, "IO data saving to local db - auto-connect service");
            ldr.saveData("Responses", response.getValues());

            if (callbacks != null) {
                Log.i(TAG, "calling service callbacks for getIOdata - auto-connect service");
                callbacks.getIOData(dataPackageInfo);
            }

        }
        if(counter%20==0){
            getPIDs();
        }
        if(counter%50==0){
            getDTCs();
        }
        if(counter%70==0){
            getPendingDTCs();
        }
        if(counter==100){
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

        Car car = new LocalCarAdapter(this).getCarByScanner(dataPackageInfo.deviceId);

        if(car != null) {
            networkHelper.getCarsById(car.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        try {
                            Car car = Car.createCar(response);

                            HashSet<String> dtcNames = new HashSet<>();
                            for (CarIssue issue : car.getActiveIssues()) {
                                dtcNames.add(issue.getIssueDetail().getItem());
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

        if (callbacks != null)
            callbacks.getIOData(dataPackageInfo);
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            Log.i(TAG,"Device login: "+loginPackageInfo.deviceId);
            Log.i(TAG,"Device result: "+loginPackageInfo.result);
            Log.i(TAG,"Device flag: "+loginPackageInfo.flag);
            currentDeviceId = loginPackageInfo.deviceId;
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

    public void startBluetoothSearch() {
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
        String savedDeviceId = getSavedSyncedDeviceId();
        if(savedDeviceId == null) {
            isGettingVin = true;
            getObdDeviceTime();
        } else {
            // Device has already been synced
            getVinFromCar();
        }
    }

    /**
     * Send command to obd device to retrieve vin from the currently
     * connected car.
     * @see #getParameterData(ParameterPackageInfo) for info returned
     * on the vin query.
     * */
    private void getVinFromCar() {
        Log.i(TAG, "Calling getCarVIN from Bluetooth auto-connect");
        bluetoothCommunicator.obdGetParameter(ObdManager.VIN_TAG);
    }


    /**
     * Send command to obd device to retrieve the current device time.
     * @see #getParameterData(ParameterPackageInfo) for device time returned
     * by obd device.
     */
    private void getObdDeviceTime() {
        Log.i(TAG, "Getting device time");
        bluetoothCommunicator.obdGetParameter(ObdManager.RTC_TAG);
    }


    /**
     * Sync obd device time with current mobile device time.
     * On successfully syncing device,  #setParameter() gets called
     * @see #setParameterResponse(ResponsePackageInfo)
     * */
    private void syncObdDevice() {
        Log.i(TAG,"Resetting RTC time - BluetoothAutoConn");
//        Toast.makeText(this,"Resetting obd device time...", Toast.LENGTH_SHORT).show();
        long systemTime = System.currentTimeMillis();
        bluetoothCommunicator
                .obdSetParameter(ObdManager.RTC_TAG, String.valueOf(systemTime / 1000));
    }

    /**
     * Store info on already synced device to reduce calls
     * to #getObdDeviceTime()
     * @param deviceId
     *          The device id of the currently connected obd device
     */
    private void saveSyncedDevice(String deviceId) {
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


    public void getPIDs(){
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

    public void getFreeze() {
        Log.i(TAG, "Getting freeze data - auto-connect service");
        bluetoothCommunicator.obdSetMonitor(ObdManager.TYPE_FREEZE_DATA, "");
    }


    private void sendForPIDS(){
        Log.i(TAG, "Sending for PIDS - auto-connect service");
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

        if(data.tripFlag.equals(ObdManager.TRIP_END_FLAG)) {
            networkHelper.saveTripMileage(data.deviceId, data.tripId, data.tripMileage, data.rtcTime,
                    new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null) {
                                Log.i(TAG, "trip data sent: " + data.tripMileage);
                                Toast.makeText(BluetoothAutoConnectService.this, "Trip data saved", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
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
        if(data.obdData.isEmpty()) {
            Log.i(TAG,"obdData is empty");
            return;
        }

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

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

        pidDataObject.setMileage(Math.max(mileage, 1)); // for overflow lol
        pidDataObject.setDataNumber(lastDataNum == null ? "" : lastDataNum);
        pidDataObject.setRtcTime(data.rtcTime);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        for(PIDInfo pidInfo : data.obdData) {
            String json  = GSON.toJson(pidInfo);
            try {
                JSONObject pid = new JSONObject();
                pid.put("id",pidInfo.pidType);
                pid.put("data",pidInfo.value);
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "PID json: " + json);
        }

        Log.d(TAG, "Pid array --> DB");
        pidDataObject.setPids(pids.toString());
        Log.d(TAG, pidDataObject.getPids());

        JSONObject freezeData = extractFreezeData(data);
        Log.d(TAG,"Freeze data --->Extract");
        Log.d(TAG,freezeData.toString());

        if(data.result == 4) {
            Log.d(TAG, "creating PID data for result 4 - " + localPidResult4.getPidDataEntryCount());
            localPidResult4.createPIDData(pidDataObject);
        } else if(data.result == 5) {
            Log.d(TAG, "creating PID data for result 5 - " + localPid.getPidDataEntryCount());
            localPid.createPIDData(pidDataObject);
        }

        if(localPid.getPidDataEntryCount() >= 100) {
            sendPidDataToServer(data);
        }

        if(localPidResult4.getPidDataEntryCount() >= 50) {
            sendPidDataResult4ToServer(data);
        }
    }

    /**
     * Send pid data to server on 100 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param data
     */
    private void sendPidDataToServer(DataPackageInfo data) {
        Log.i(TAG, "sending PID data");
        List<Pid> pidDataEntries = localPid.getAllPidDataEntries();
        JSONArray pidArray = new JSONArray();

        try {
            for(Pid pidDataObject : pidDataEntries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum", pidDataObject.getDataNumber());
                jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                jsonObject.put("mileage", pidDataObject.getMileage());
                jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                pidArray.put(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.savePids(data.deviceId, pidArray,
                new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            Log.i(TAG, "PIDS saved");
                            localPid.deleteAllPidDataEntries();
                        } else {
                            Log.e(TAG, "save pid error: " + requestError.getMessage());
                            if(requestError.getStatusCode() == 400) {
                                localPid.deleteAllPidDataEntries();
                            }
                        }
                    }
                });
    }

    /**
     * Send pid data on result 4 to server on 50 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param data
     */
    private void sendPidDataResult4ToServer(DataPackageInfo data) {
        Log.i(TAG, "sending PID result 4 data");
        List<Pid> pidDataEntries = localPidResult4.getAllPidDataEntries();
        JSONArray pidArray = new JSONArray();

        try {
            for(Pid pidDataObject : pidDataEntries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum", pidDataObject.getDataNumber());
                jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                jsonObject.put("mileage", pidDataObject.getMileage());
                jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                pidArray.put(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.savePids(data.deviceId, pidArray,
                new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            Log.i(TAG, "PIDS result 4 saved");
                            localPidResult4.deleteAllPidDataEntries();
                        } else {
                            Log.e(TAG, "save pid result 4 error: " + requestError.getMessage());
                            if(requestError.getStatusCode() == 400) {
                                localPid.deleteAllPidDataEntries();
                            }
                        }
                    }
                });
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

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                Log.i(TAG, "Bond state changed: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                if (bluetoothCommunicator instanceof BluetoothLeComm && intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                    ((BluetoothLeComm) bluetoothCommunicator).connectToDevice((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                }
            } else if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i(TAG, "Bluetooth adapter state changed: " + state);
                bluetoothCommunicator.bluetoothStateChanged(state);
                if(state == BluetoothAdapter.STATE_OFF) {
                    bluetoothCommunicator.close();
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(notifID);
                } else if(state == BluetoothAdapter.STATE_ON && BluetoothAdapter.getDefaultAdapter() != null) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        bluetoothCommunicator = new BluetoothLeComm(BluetoothAutoConnectService.this);
                    } else {
                        bluetoothCommunicator = new BluetoothClassicComm(BluetoothAutoConnectService.this);
                    }

                    bluetoothCommunicator.setBluetoothDataListener(BluetoothAutoConnectService.this);
                    if (BluetoothAdapter.getDefaultAdapter()!=null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        bluetoothCommunicator.startScan();
                    }
                }
            }
        }
    };
}

package com.pitstop.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
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
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.pitstop.DataAccessLayer.DTOs.Pid;
import com.pitstop.DataAccessLayer.DataAdapters.PidAdapter;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Responses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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
    private boolean askforDtcs = false;
    private boolean askForPendingDTCs = false;
    private boolean deviceConnState = false;

    private int notifID = 1360119;
    private String currentDeviceId = null;

    private ParseObject tripMileage = null;
    private HashMap<String, String> tripData = new HashMap<>();

    private int counter = 1;
    private int status5counter = 0;

    String[] pids = new String[0];
    int pidI = 0;

    private PidAdapter localPid;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(MainActivity.TAG, "BluetoothAutoConnect#OnCreate()");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothCommunicator = new BluetoothLeComm(this);
        } else {
            bluetoothCommunicator = new BluetoothClassicComm(this);
        }

        bluetoothCommunicator.setBluetoothDataListener(this);
        localPid = new PidAdapter(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MainActivity.TAG, "Running on start command - auto-connect service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(MainActivity.TAG, "Destroying auto-connect service");
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
        Log.i(MainActivity.TAG, "Getting bluetooth state - auto-connect service");
        if(state==IBluetoothCommunicator.CONNECTED) {
            Log.i(MainActivity.TAG,"Bluetooth state connected - auto-connect service");
            Log.i(MainActivity.TAG,"getting bonded devices - auto-connect service");
            BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            List<BluetoothDevice> devices = new LinkedList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            }
            boolean deviceConnected = false;
            for (BluetoothDevice device : devices) {
                Log.i(MainActivity.TAG,"Iterating through bonded devices - auto-connect service");
                //if device has name IDD-212
                if (device.getName().contains("IDD-212")) {
                    Log.i(MainActivity.TAG,"Found connected device - auto-connect service");
                    deviceConnected = true;
                }
            }
            //show a custom notification
            if (deviceConnected) {
                Log.i(MainActivity.TAG,"Device is connected -  auto-connect service");
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                                .setColor(getResources().getColor(R.color.highlight))
                                .setContentTitle("Car is Connected")
                                .setContentText("Click here to check out more");
                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(this, MainActivity.class);

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
                Log.i(MainActivity.TAG, "sending out car is connected notification - auto-connect service");
                mNotificationManager.notify(notifID, mBuilder.build());
            } else {
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Log.i(MainActivity.TAG, "cancelling car is connected notification");
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
            if(tripMileage!=null) {

                tripMileage.put("bluetoothConnection", "disconnected");
                tripMileage.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e!=null) {
                            Log.i(MainActivity.TAG,"Trip mil error: "+e.getMessage());
                        } else {
                            Log.i(MainActivity.TAG, "Saved trip mileage");
                        }

                    }
                });

                tripMileage = null;
            }

        }

        if (callbacks != null) {
            Log.i(MainActivity.TAG, "Calling service callbacks to getBluetooth State - auto connect service");
            callbacks.getBluetoothState(state);
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if(callbacks != null) {
            Log.i(MainActivity.TAG,"Setting ctrl response on service callbacks - auto-connect service");
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
            Log.i(MainActivity.TAG, "Setting parameter response on service callbacks - auto-connect service");
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

        if(gettingPID){
            Log.i(MainActivity.TAG,"Getting parameter data- auto-connect service");
            pids  =parameterPackageInfo.value.get(0).value.split(",");
            pidI = 0;
            sendForPIDS();
            gettingPID=false;
        } else if(isGettingVin) {
            if(parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {
                long moreThanOneYear = 32000000;
                long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
                long currentTime = System.currentTimeMillis()/1000;
                long diff = currentTime - deviceTime;
                if(diff > moreThanOneYear) {
                    syncObdDevice();
                } else {
                    saveSyncedDevice(currentDeviceId);
                    getVinFromCar();
                    isGettingVin = false;
                }
            }

        }

        if(callbacks != null) {
            Log.i(MainActivity.TAG, "getting parameter data on service Callbacks - auto-connect service");
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

        deviceConnState = true;
        currentDeviceId = dataPackageInfo.deviceId;
        processPIDData(dataPackageInfo);

        if(dataPackageInfo.result == 4) {
            processResultFourData(dataPackageInfo);
        }

        if(dataPackageInfo.result == 5) {
            //processResultFiveData(dataPackageInfo);
        }

        if(dataPackageInfo.result == 6) {
            //processResultSixData(dataPackageInfo);
        }

        Log.i(MainActivity.TAG, "getting io data - auto-connect service");
        if (dataPackageInfo.result != 5&&dataPackageInfo.result!=4&&askforDtcs) {
            askforDtcs=false;
            String dtcs = "";
            if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for(String dtc : DTCs) {
                    dtcs+= ObdDataUtil.parseDTCs(dtc)+",";
                }
            }
            //update DTC to online
            ParseObject scansSave = new ParseObject("Scan");
            scansSave.put("DTCs", dtcs);
            scansSave.put("scannerId", dataPackageInfo.deviceId);
            scansSave.put("runAfterSave", true);
            scansSave.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Log.d("DTC Saving", "DTCs saved");
                }
            });
            if (callbacks != null)
                callbacks.getIOData(dataPackageInfo);
            return;
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

        //TODO refactor:
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Responses response = new Responses();

        // Todo: @dataPackage result will never be 1,2, or 3
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
            // TOdo
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
            Log.i(MainActivity.TAG, "IO data saving to local db - auto-connect service");
            ldr.saveData("Responses", response.getValues());

            if (callbacks != null) {
                Log.i(MainActivity.TAG, "calling service callbacks for getIOdata - auto-connect service");
                callbacks.getIOData(dataPackageInfo);
            }

        }
        if(counter%20==0){
            getPIDs();
        }
        if(counter%50==0){
            getDTCs();
        }
        if(counter==100){
            counter = 1;
            //uploadRecords();
        }

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            Log.i(MainActivity.TAG,"Device login: "+loginPackageInfo.deviceId);
            Log.i(MainActivity.TAG,"Device result: "+loginPackageInfo.result);
            Log.i(MainActivity.TAG,"Device flag: "+loginPackageInfo.flag);
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

        String savedDeviceId = getSavedSyncedDeviceId();
        if(TextUtils.isEmpty(savedDeviceId) || currentDeviceId==null
                || !currentDeviceId.equals(savedDeviceId)) {
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
        Log.i(MainActivity.TAG, "Calling getCarVIN from Bluetooth auto-connect");
        bluetoothCommunicator.obdGetParameter(ObdManager.VIN_TAG);
    }


    /**
     * Send command to obd device to retrieve the current device time.
     * @see #getParameterData(ParameterPackageInfo) for device time returned
     * by obd device.
     */
    private void getObdDeviceTime() {
        Log.i(MainActivity.TAG, "Getting device time");
        bluetoothCommunicator.obdGetParameter(ObdManager.RTC_TAG);
    }


    /**
     * Sync obd device time with current mobile device time.
     * On successfully syncing device,  #setParameter() gets called
     * @see #setParameterResponse(ResponsePackageInfo)
     * */
    private void syncObdDevice() {
        Log.i(MainActivity.TAG,"Resetting RTC time - BluetoothAutoConn");
        Toast.makeText(this,"Resetting obd device time...", Toast.LENGTH_SHORT).show();
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
        return sharedPreferences.getString(DEVICE_ID,"");
    }

    public int getState() {
        Log.i(MainActivity.TAG, "getting bluetooth state - auto-connect service");
        return bluetoothCommunicator.getState();
    }


    public void getPIDs(){
        Log.i(MainActivity.TAG,"getting PIDs - auto-connect service");
        bluetoothCommunicator.obdGetParameter(ObdManager.PID_TAG);
        gettingPID = true;
    }

    public void getDTCs() {
        Log.i(MainActivity.TAG, "calling getting DTCs - auto-connect service");
        if (!askforDtcs){
            askforDtcs = true;
            bluetoothCommunicator.obdSetMonitor(1, "");
        }
    }


    public void getPendingDTCs() {
        Log.i(MainActivity.TAG, "Getting pending DTCs");
        if (!askForPendingDTCs){
            askForPendingDTCs = true;
            bluetoothCommunicator.obdSetMonitor(2, "");
        }
    }


    public void getFreeze() {
        Log.i(MainActivity.TAG, "Getting freeze data - auto-connect service");
        bluetoothCommunicator.obdSetMonitor(3, "");
    }


    private void sendForPIDS(){
        Log.i(MainActivity.TAG, "Sending for PIDS - auto-connect service");
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
    private void processResultFourData(DataPackageInfo data) {
        if(tripMileage==null) {
            tripMileage = new ParseObject("TripMileage");
            tripData.clear();
        }

        int mileage = 0;
        if(data.tripMileage != null && !"".equals(data.tripMileage)) {
            mileage = Integer.parseInt(data.tripMileage)/1000;
        }

        tripMileage.put("tripId", Integer.parseInt(data.tripId));
        tripMileage.put("scannerId", data.deviceId);
        tripMileage.put("mileage", mileage);
        tripMileage.put("bluetoothConnection",
                getState() == IBluetoothCommunicator.CONNECTED ? "connected" : "disconnected");
        tripMileage.put("rtcTime", data.rtcTime);
        tripMileage.put("tripFlag", data.tripFlag);
        tripMileage.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        tripData.put("dataNumber", data.dataNumber);
        tripMileage.put("tripData",tripData);

        if(data.tripFlag.equals(ObdManager.TRIP_END_FLAG)) {

            tripMileage.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.i(MainActivity.TAG, "Error: " + e.getMessage());
                    } else {
                        Log.i(MainActivity.TAG, "Saved trip mileage");
                    }
                }
            });
            tripMileage = null;
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
            Log.i(MainActivity.TAG,"obdData is empty");
            return;
        }

        if(localPid.getPidDataEntryCount() == 100) {
            sendPidDataToServer(data);
        }

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        pidDataObject.setDataNumber(data.dataNumber == null ? "" : data.dataNumber);
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
            Log.i(MainActivity.TAG,json);
        }

        Log.i(MainActivity.TAG, "Pid array --> DB");
        pidDataObject.setPids(pids.toString());
        Log.i(MainActivity.TAG, pidDataObject.getPids());

        JSONObject freezeData = extractFreezeData(data);
        Log.i(MainActivity.TAG,"Freeze data --->Extract");
        Log.i(MainActivity.TAG,freezeData.toString());

        localPid.createPIDData(pidDataObject);
    }

    /**
     * Send pid data to server on 100 data points received
     * @see #processPIDData(DataPackageInfo)
     * @param data
     */
    private void sendPidDataToServer(DataPackageInfo data) {
        ParseObject pidScanTable = new ParseObject("Scan");
        double tripMileage = 0;
        if(data.tripMileage != null && !"".equals(data.tripMileage)) {
            tripMileage = Double.parseDouble(data.tripMileage)/1000;
        }
        pidScanTable.put("mileage", tripMileage);
        pidScanTable.put("scannerId", data.deviceId);

        List<Pid> pidDataEntries = localPid.getAllPidDataEntries();

        JSONArray pidArray = null;

        try {
            pidArray = new JSONArray();
            for(Pid pidDataObject : pidDataEntries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum",pidDataObject.getDataNumber());
                jsonObject.put("rtcTime",pidDataObject.getRtcTime());
                jsonObject.put("timestamp",pidDataObject.getTimeStamp());
                jsonObject.put("pids",new JSONArray(pidDataObject.getPids()));
                pidArray.put(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(MainActivity.TAG,"Array --> backend");
        Log.i(MainActivity.TAG,pidArray.toString());

        JSONObject freezeData = extractFreezeData(data);

        pidScanTable.put("PIDArray", pidArray);
        pidScanTable.put("freezeDataArray",freezeData);
        pidScanTable.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.i(MainActivity.TAG, "Saved successfully");
                    localPid.deleteAllPidDataEntries();
                } else {
                    Log.i(MainActivity.TAG, e.getMessage());
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
        JSONArray pids = new JSONArray();

        if(data.freezeData.isEmpty()) {
            Log.i(MainActivity.TAG,"No freeze Data");
            return jsonObject;
        }

        try {
            jsonObject.put("dataNum",data.dataNumber==null ? "" : data.dataNumber);
            jsonObject.put("rtcTime", data.rtcTime);
            jsonObject.put("timestamp",String.valueOf(System.currentTimeMillis()/1000));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(PIDInfo pidInfo : data.freezeData) {
            String json = GSON.toJson(pidInfo);
            Log.i("Freeze Data","-"+json);
            try {
                pids.put(new JSONObject(json));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            jsonObject.put("pids",pids);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}

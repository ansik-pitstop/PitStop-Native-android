package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothClassicComm;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;

import com.pitstop.models.Pid;
import com.pitstop.utils.MessageListener;
import com.pitstop.utils.TestTimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener {

    private final IBinder mBinder = new BluetoothBinder();
    private IBluetoothCommunicator bluetoothCommunicator;
    private MessageListener callbacks;

    public static int notifID = 1360119;
    private String currentDeviceId = null;

    private State state = State.NONE;

    public enum State {
        NONE, CONNECTING, GET_RTC, VERIFY_RTC, READ_PIDS, READ_DTCS, GET_VIN, RESET
    }

    private TestTimer testTimer = null;

    int vinAttempts = 0;

    boolean rtcSuccess = false;
    boolean pidSuccess = false;
    boolean dtcSuccess = false;
    boolean vinSuccess = false;

    private static String TAG = "BtAutoConnectDebug";

    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();
    private String supportedPids = "";
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        if(BluetoothAdapter.getDefaultAdapter() != null) {
            bluetoothCommunicator = new BluetoothClassicComm(this);
            bluetoothCommunicator.setBluetoothDataListener(this);
            if (BluetoothAdapter.getDefaultAdapter()!=null
                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                BluetoothAdapter.getDefaultAdapter().enable();
                startBluetoothSearch(3);  // start search when service starts
            }
        }

        initPidPriorityList();

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Running on start command - auto-connect service");
        return START_NOT_STICKY;
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
        if (callbacks != null) {
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if(callbacks != null) {
        }
    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        if(state == State.VERIFY_RTC) {
            sendMessageToUi(MessageListener.STATUS_UPDATE, "Device set time, syncing device...");
            getVinFromCar();
        } else if(state == State.RESET) {
            //bluetoothCommunicator.obdSetCtrl();
        }
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
        if(state == State.GET_RTC) {
            if(parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {
                final long moreThanOneYear = 32000000;
                final long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
                final long currentTime = System.currentTimeMillis() / 1000;
                final long diff = currentTime - deviceTime;

                String readableDate = new SimpleDateFormat("MM d, yyyy").format(new Date(deviceTime * 1000));

                sendMessageToUi(MessageListener.STATUS_UPDATE, "Device time received:" + readableDate);

                if (diff > moreThanOneYear) {
                    state = State.VERIFY_RTC;
                    sendMessageToUi(MessageListener.STATUS_UPDATE, "Need to set device time");
                    vinAttempts = 0;
                    syncObdDevice();
                } else {
                    Log.i(TAG, "RTC success");
                    rtcSuccess = true;
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Device time does not need to be set");
                    listenForPids();
                }
            }
        } else if(state == State.VERIFY_RTC) {
            if(parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {
                if(parameterPackageInfo.value.get(0).value.length() == 17) { // successfully synced device
                    Log.i(TAG, "RTC success");
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Device successfully synced");
                    rtcSuccess = true;
                    listenForPids();
                } else if(vinAttempts++ < 6) { // may still be syncing, try again
                    sendMessageToUi(MessageListener.STATUS_UPDATE, "Syncing device...");
                    if(testTimer != null) {
                        testTimer.cancel();
                    }
                    testTimer = new TestTimer(10000) {
                        @Override
                        public void onFinish() {
                            getVinFromCar();
                        }
                    };
                    testTimer.start();
                } else { // vin not retrievable
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not verify device sync");
                    listenForPids();
                }
            }
        } else if(state == State.GET_VIN) {
            if(parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {
                if(parameterPackageInfo.value.get(0).value.length() == 17) {
                    vinSuccess = true;
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "VIN retrieved successfully: " + parameterPackageInfo.value.get(0).value);
                } else {
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get valid VIN");
                }
            }
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.v(TAG, "dtcData: " + dataPackageInfo.dtcData);
        callbacks.connectSuccess();
        if(state == State.READ_PIDS && dataPackageInfo.result == 5
                && dataPackageInfo.obdData != null && dataPackageInfo.obdData.size() > 0) {
            Log.i(TAG, "PID Success");
            pidSuccess = true;

            StringBuilder pids = new StringBuilder();
            for(PIDInfo pid : dataPackageInfo.obdData) {
                pids.append("\n");
                pids.append(pid.pidType);
                pids.append(": ");
                pids.append(pid.value);
            }

            sendMessageToUi(MessageListener.STATUS_UPDATE, "Data retrieved: " + pids.toString());
            sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received sensor data");
            testTimer.cancel();
            listenForDtcs();
        } else if(state == State.READ_DTCS && dataPackageInfo.dtcData != null) {
            Log.i(TAG, "DTC Success");
            dtcSuccess = true;
            sendMessageToUi(MessageListener.STATUS_UPDATE, "Engine codes received: " + dataPackageInfo.dtcData);
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            Log.i(TAG,"Device login: "+loginPackageInfo.deviceId);
            Log.i(TAG,"Device result: "+loginPackageInfo.result);
            Log.i(TAG,"Device flag: "+loginPackageInfo.flag);
            currentDeviceId = loginPackageInfo.deviceId;
            bluetoothCommunicator.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);
            callbacks.connectSuccess();
        } else if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }
    }

    // test pids for 20 seconds
    private void listenForPids() {
        state = State.READ_PIDS;
        if(testTimer != null) {
            testTimer.cancel();
        }
        testTimer = new TestTimer(12000) {
            @Override
            public void onFinish() {
                if(pidSuccess) {
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received sensor data");
                } else {
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get sensor data");
                }
                listenForDtcs();
            }
        };
        testTimer.start();
    }

    // test dtcs for 20 seconds
    private void listenForDtcs() {
        state = State.READ_DTCS;
        if(testTimer != null) {
            testTimer.cancel();
        }
        testTimer = new TestTimer(15000) {
            @Override
            public void onFinish() {
                if(dtcSuccess) {
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received engine codes");
                } else {
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get engine codes");
                }
                state = State.GET_VIN;
                getVinFromCar();
            }
        };
        testTimer.start();
    }

    private void sendMessageToUi(int status, String message) {
        if(callbacks != null) {
            callbacks.processMessage(status, state, message);
        }
    }

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void setCallbacks(MessageListener callbacks) {
        this.callbacks = callbacks;
    }

    public void startBluetoothSearch(int... source) {
        Log.d(TAG, "startBluetoothSearch() " + ((source != null && source.length > 0) ? source[0] : ""));
        if (bluetoothCommunicator == null) {
            bluetoothCommunicator = new BluetoothClassicComm(this);
            bluetoothCommunicator.setBluetoothDataListener(this);
        }
        bluetoothCommunicator.startScan();
    }

    public void disconnectFromDevice() {
        Log.i(TAG, "Disconnecting from device");
        if(testTimer != null) {
            testTimer.cancel();
        }
        if(bluetoothCommunicator != null) {
            bluetoothCommunicator.close();
            bluetoothCommunicator = null;
        }
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
        state = State.GET_RTC;
        sendMessageToUi(MessageListener.STATUS_UPDATE, "Getting device time");
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

    public int getState() {
        return bluetoothCommunicator.getState();
    }


    public void getPIDs(){ // supported pids
        Log.i(TAG,"getting PIDs - auto-connect service");
        bluetoothCommunicator.obdGetParameter(ObdManager.PID_TAG);
    }

    public void getDTCs() {
        Log.i(TAG, "calling getting DTCs - auto-connect service");
        bluetoothCommunicator.obdSetMonitor(ObdManager.TYPE_DTC, "");
    }

    public void getPendingDTCs() {
        Log.i(TAG, "Getting pending DTCs");
        bluetoothCommunicator.obdSetMonitor(ObdManager.TYPE_PENDING_DTC, "");
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
                        bluetoothCommunicator = new BluetoothClassicComm(BluetoothAutoConnectService.this);
                    } else {
                        bluetoothCommunicator = new BluetoothClassicComm(BluetoothAutoConnectService.this);
                    }

                    bluetoothCommunicator.setBluetoothDataListener(BluetoothAutoConnectService.this);
                    if (BluetoothAdapter.getDefaultAdapter()!=null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startBluetoothSearch(6); // start search when turning bluetooth on
                    }
                }
            }
        }
    };

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

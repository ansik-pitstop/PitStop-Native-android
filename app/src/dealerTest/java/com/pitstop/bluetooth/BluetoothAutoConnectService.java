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

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;

import com.castel.obd.util.ObdDataUtil;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.TestDatabaseHelper;
import com.pitstop.database.TestPidAdapter;
import com.pitstop.models.Dtc;
import com.pitstop.models.DtcPayload;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MessageListener;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.RandomUtils;
import com.pitstop.utils.TestTimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Paul Soladoye on 11/04/2016.
 */
public class BluetoothAutoConnectService extends Service implements ObdManager.IBluetoothDataListener {

    private static final String TAG = BluetoothAutoConnectService.class.getSimpleName();

    private final IBinder mBinder = new BluetoothBinder();
    private IBluetoothCommunicator bluetoothCommunicator;
    private MessageListener callbacks;

    private GlobalApplication application;

    public static int notifID = 1360119;
    private String currentDeviceId = null;

    private State state = State.NONE;

    public enum State {
        NONE, CONNECTING, GET_RTC, VERIFY_RTC, GET_VIN, READ_PIDS, READ_DTCS, COLLECT_DATA, RESET, FINISH
    }

    private TestTimer testTimer = null;

    int vinAttempts = 0;
    int rtcAttempts = 0;

    boolean rtcSuccess = false;
    boolean vinSuccess = false;
    boolean pidSuccess = false;
    boolean dtcSuccess = false;
    boolean dataSuccess = false;

    private NetworkHelper mNetworkHelper;

    private TestPidAdapter mTestPidAdapter;
    private int PID_CHUNK_SIZE = 100;
    private int counter = 1;
    private String[] pids = new String[0];
    private int pidI = 0;

    private DataPackageInfo lastData = null;
    private String lastDataNum = "";

    private int lastDeviceTripId = -1; // from device
    private final String pfDeviceTripId = "lastDeviceTripId";

    private ArrayList<DtcPayload> dtcsToSend = new ArrayList<>();

    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();
    private String supportedPids = "";
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";

    // Last stored pids
    private List<PIDInfo> mPidSnapshots;
    private boolean isSendingPids = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BluetoothAutoConnect#OnCreate()");

        application = (GlobalApplication) getApplicationContext();

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            bluetoothCommunicator = new BluetoothTestAppComm(this);
            bluetoothCommunicator.setBluetoothDataListener(this);
            if (BluetoothAdapter.getDefaultAdapter() != null
                    && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                BluetoothAdapter.getDefaultAdapter().enable();
                startBluetoothSearch(3);  // start search when service starts
            }
        }

        initPidPriorityList();

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);

        mNetworkHelper = new NetworkHelper(getApplicationContext());
        mTestPidAdapter = new TestPidAdapter(getApplicationContext());
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
        if (state == IBluetoothCommunicator.DISCONNECTED &&
                (this.state == State.COLLECT_DATA)) {
            sendMessageToUi(MessageListener.STATUS_FAILED, "Bluetooth has disconnected, uploading data");
            dtcsToSend.clear();
            flushData();
        } else if (state == IBluetoothCommunicator.CONNECTED &&
                this.state == State.CONNECTING) {
            callbacks.connectSuccess();
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if (callbacks != null) {
        }
    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        if (state == State.VERIFY_RTC) {
            sendMessageToUi(MessageListener.STATUS_UPDATE, "Device set time, syncing device...");
            getVinFromCar();
        } else if (state == State.COLLECT_DATA) {
            Log.i(TAG, "Resetting device");
            bluetoothCommunicator.obdSetCtrl(3);
            bluetoothCommunicator.obdSetCtrl(2); // clear pids
            disconnectFromDevice();
        }
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
        if (state == State.GET_RTC) {
            if (parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {
                final long moreThanOneYear = 32000000;
                final long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
                final long currentTime = System.currentTimeMillis() / 1000;
                final long diff = currentTime - deviceTime;

                String readableDate = new SimpleDateFormat("MM d, yyyy").format(new Date(deviceTime * 1000));

                sendMessageToUi(MessageListener.STATUS_UPDATE, "Device time received:" + readableDate);

                if (diff > moreThanOneYear) {
                    vinAttempts = 0;
                    state = State.VERIFY_RTC;
                    syncObdDevice();
                    sendMessageToUi(MessageListener.STATUS_UPDATE, "Need to set device time...");
                } else {
                    Log.i(TAG, "RTC success");
                    rtcSuccess = true;
                    state = State.GET_VIN;
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Device time does not need to be set");
                    getVinFromCar();
                }
            }
        } else if (state == State.VERIFY_RTC) {
            if (parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {
                if (parameterPackageInfo.value.get(0).value.length() == 17) { // successfully synced device
                    Log.i(TAG, "RTC success");
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Device successfully synced");
                    rtcSuccess = true;
                    state = State.GET_VIN;
                    getVinFromCar();
                } else if (vinAttempts++ < 6) { // may still be syncing, try again
                    sendMessageToUi(MessageListener.STATUS_UPDATE, "Syncing device...");
                    if (testTimer != null) {
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
                    Log.d(TAG, "VERIFY_RTC FAILED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not verify device sync");
                    listenForPids();
                }
            }
        } else if (state == State.GET_VIN) {
            if (parameterPackageInfo.value.size() > 0 && parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {
                if (parameterPackageInfo.value.get(0).value.length() == 17) {
                    vinSuccess = true;
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "VIN retrieved successfully: " + parameterPackageInfo.value.get(0).value);
                    NetworkHelper.setVIN(parameterPackageInfo.value.get(0).value);
                    NetworkHelper.setTripId(RandomUtils.getRandomTripId());
                    listenForPids();
                } else if (vinAttempts++ < 6) { // may still be syncing, try again
                    sendMessageToUi(MessageListener.STATUS_UPDATE, "Waiting for Vin...");
                    if (testTimer != null) {
                        testTimer.cancel();
                    }
                    testTimer = new TestTimer(10000) {
                        @Override
                        public void onFinish() {
                            getVinFromCar();
                        }
                    };
                    testTimer.start();
                } else { // Cannot get vin
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get valid VIN");
                    String generatedVin = RandomUtils.getRandomStringAsVin(RandomUtils.VIN_LENGTH);
                    Log.d(TAG, "VIN generated: " + generatedVin);
                    NetworkHelper.setVIN(generatedVin);
                    NetworkHelper.setTripId(RandomUtils.getRandomTripId());
                    listenForPids();
                }
            }
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

        Log.v(TAG, "dtcData: " + dataPackageInfo.dtcData);

        if (state == State.CONNECTING) {
            callbacks.connectSuccess();
        }

        if (state == State.READ_PIDS && dataPackageInfo.result == 5
                && dataPackageInfo.obdData != null
                && dataPackageInfo.obdData.size() > 0) {
            Log.i(TAG, "PID Success");
            pidSuccess = true;

            StringBuilder pids = new StringBuilder();
            for (PIDInfo pid : dataPackageInfo.obdData) {
                pids.append("\n");
                pids.append(pid.pidType);
                pids.append(": ");
                pids.append(pid.value);
            }

            sendMessageToUi(MessageListener.STATUS_UPDATE, "Data retrieved: " + pids.toString());
            sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received sensor data");

            testTimer.cancel();
            listenForDtcs();

        } else if (state == State.READ_DTCS && dataPackageInfo.dtcData != null) {
            Log.i(TAG, "DTC Success");

            dtcSuccess = true;

            if (!dataPackageInfo.dtcData.isEmpty()) {
                sendMessageToUi(MessageListener.STATUS_SUCCESS, "Engine codes received: " + dataPackageInfo.dtcData);
            } else {
                sendMessageToUi(MessageListener.STATUS_SUCCESS, "Engine codes received is empty");
            }

            testTimer.cancel();
            startCollectingData();

        } else if (state == State.COLLECT_DATA) {

            if (dataPackageInfo.result == 5 && dataPackageInfo.obdData != null
                    && dataPackageInfo.obdData.size() > 0) { // PIDs

                mPidSnapshots = dataPackageInfo.obdData; // Store pid as a snapshot
                processPIDData(dataPackageInfo);

            } else if (dataPackageInfo.result == 6 && dataPackageInfo.dtcData != null) { // DTCs
                saveDtcs(dataPackageInfo, false);
            } else if (dataPackageInfo.tripFlag != null && dataPackageInfo.tripFlag.equals("5")) { // DTCs
                saveDtcs(dataPackageInfo, false);
            } else if (dataPackageInfo.tripFlag != null && dataPackageInfo.tripFlag.equals("6")) { // Pending DTCs
                saveDtcs(dataPackageInfo, true);
            }

            counter++;

            if (counter == 50) {
                getPIDs();
            }

            if (counter % 30 == 0) {
                getDTCs();
            }

            if (counter % 70 == 0) {
//                getPendingDTCs();
                getDTCs();
            }

            if (counter == 150) {
                counter = 1;
                flushData();
                dataSuccess = true;
                testTimer.cancel();
                sendMessageToUi(MessageListener.STATUS_SUCCESS, "Finished collecting data");
            }
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            Log.i(TAG, "Device login: " + loginPackageInfo.deviceId);
            Log.i(TAG, "Device result: " + loginPackageInfo.result);
            Log.i(TAG, "Device flag: " + loginPackageInfo.flag);
            currentDeviceId = loginPackageInfo.deviceId;
            bluetoothCommunicator.bluetoothStateChanged(IBluetoothCommunicator.CONNECTED);
            if (state == State.CONNECTING) {
                callbacks.connectSuccess();
            }
        } else if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            currentDeviceId = null;
        }
    }

    // test pids for 20 seconds
    private void listenForPids() {
        state = State.READ_PIDS;
        if (testTimer != null) {
            testTimer.cancel();
        }
        testTimer = new TestTimer(12000) {
            @Override
            public void onFinish() {
                if (pidSuccess) {
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received sensor data");
                } else {
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get sensor data");
                }
                listenForDtcs(); // When finished, start trying to get DTCs
            }
        };
        testTimer.start();
    }

    // test dtcs for 20 seconds
    private void listenForDtcs() {
        state = State.READ_DTCS;
        getDTCs();
        if (testTimer != null) {
            testTimer.cancel();
        }
        testTimer = new TestTimer(15000) {
            @Override
            public void onFinish() {
                if (dtcSuccess) {
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully received engine codes");
                } else {
                    sendMessageToUi(MessageListener.STATUS_FAILED, "Could not get engine codes");
                }
                startCollectingData(); // When finished, start collecting data
            }
        };
        testTimer.start();
    }

    /**
     * Starts collecting pid data
     */
    private void startCollectingData() {
        state = State.COLLECT_DATA;
        getPIDs();
        if (testTimer != null) {
            testTimer.cancel();
        }
        testTimer = new TestTimer(120000) {
            @Override
            public void onFinish() {
                if (!dataSuccess) {
                    counter = 1;
                    dataSuccess = true;
                    flushData();
                    testTimer.cancel();
                    sendMessageToUi(MessageListener.STATUS_SUCCESS, "Finished collecting data");
                }
            }
        };
        testTimer.start();
    }

    /**
     * Process data package sent from device for pids. Store pids locally, once
     * we have 100 data points, send the data to the server.
     *
     * @param data The OBD data package that possibly contains obdData i.e pids
     */
    private void processPIDData(DataPackageInfo data) {
        if (data.obdData.isEmpty()) {
            Log.i(TAG, "obdData is empty");
            return;
        }

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

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

        mTestPidAdapter.createPIDData(pidDataObject);

        if (mTestPidAdapter.getPidDataEntryCount() >= PID_CHUNK_SIZE
                && mTestPidAdapter.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer();
        }

    }

    /**
     * Send pid data to server on 100 data points received, clear local database after doing so
     *
     * @see #processPIDData(DataPackageInfo)
     */
    private void sendPidDataToServer() {
        if (isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID data");
        sendMessageToUi(MessageListener.STATUS_UPDATE, "Uploading PIDs");
        List<Pid> pidDataEntries = mTestPidAdapter.getAllPidDataEntries();

        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];

        try {
            for (int chunkNumber = 0; chunkNumber < chunks; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                JSONObject item = new JSONObject();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }
                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (JSONArray pids : pidArrays) {
            if (pids.length() == 0) {
                continue;
            }
            mNetworkHelper.postTestPids(pids, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    isSendingPids = false;
                    if (requestError == null) {
                        Log.i(TAG, "PIDS saved");
                        mTestPidAdapter.deleteAllPidDataEntries();
                    } else {
                        Log.e(TAG, "save pid error: " + requestError.getMessage());
                        if (getDatabasePath(TestDatabaseHelper.DATABASE_NAME).length() > 10000000L) { // delete pids if db size > 10MB
                            mTestPidAdapter.deleteAllPidDataEntries();
                        }
                    }
                }
            });
        }
    }

    private void saveDtcs(final DataPackageInfo dataPackageInfo, final boolean isPendingDtc) {
        Log.i(TAG, "save DTCs - auto-connect service");
        sendMessageToUi(MessageListener.STATUS_UPDATE, "Uploading DTCs");
        String dtcs = "";
        final ArrayList<String> dtcArr = new ArrayList<>();
        if (dataPackageInfo.dtcData != null && dataPackageInfo.dtcData.length() > 0) {
            String[] DTCs = dataPackageInfo.dtcData.split(",");
            for (String dtc : DTCs) {
                String parsedDtc = ObdDataUtil.parseDTCs(dtc);
                dtcs += parsedDtc + ",";
                dtcArr.add(parsedDtc);
            }
        }

        JSONArray pidArr = new JSONArray();
        try {
            JSONObject item = new JSONObject();
            item.put("rtcTime", dataPackageInfo.rtcTime);
            JSONArray pids = new JSONArray();
            for (PIDInfo info : mPidSnapshots) {
                JSONObject pid = new JSONObject();
                pid.put("id", info.pidType).put("data", info.pidType);
                pids.put(pid);
            }
            item.put("pids", pids);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "DTCs found: " + dtcs);

        if (NetworkHelper.isConnected(this)) {
            for (final String dtc : dtcArr) {
                mNetworkHelper.postTestDtcs(pidArr, dtc, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        Log.d(TAG, "DTC posted: " + dtc);
                    }
                });
            }
        } else {
            // Save it offline
            for (final String dtc : dtcArr) {
                dtcsToSend.add(new DtcPayload(pidArr, dtc));
            }
        }

    }

    /**
     * This method dumps all pid data currently stored in the database to the endpoint
     */
    private void flushData() {
        if (isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID data");
        List<Pid> pidDataEntries = mTestPidAdapter.getAllPidDataEntries();

        if (pidDataEntries.size() == 0) return;

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
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (JSONArray pids : pidArrays) {
            if (pids.length() == 0) {
                continue;
            }
            mNetworkHelper.postTestPids(pids, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    isSendingPids = false;
                    mTestPidAdapter.deleteAllPidDataEntries();
                    if (requestError == null) {
                        Log.i(TAG, "PIDS saved");
                        sendMessageToUi(MessageListener.STATUS_SUCCESS, "Successfully uploaded data");
                    } else {
                        Log.e(TAG, "save pid error: " + requestError.getMessage());
                        sendMessageToUi(MessageListener.STATUS_FAILED, "Error uploading data");
                    }
                }
            });
        }

    }


    private void sendMessageToUi(int status, String message) {
        if (callbacks != null) {
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
            bluetoothCommunicator = new BluetoothTestAppComm(this);
            bluetoothCommunicator.setBluetoothDataListener(this);
        }
        bluetoothCommunicator.startScan();
    }

    public void disconnectFromDevice() {
        Log.i(TAG, "Disconnecting from device");
        if (testTimer != null) {
            testTimer.cancel();
        }
        if (bluetoothCommunicator != null) {
            bluetoothCommunicator.close();
            bluetoothCommunicator = new BluetoothTestAppComm(this);
            bluetoothCommunicator.setBluetoothDataListener(this);
        }
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
     * @see #getParameterData(ParameterPackageInfo)
     */
    public void getCarVIN() {
        Log.i(TAG, "getCarVin");
        getObdDeviceTime();
    }

    /**
     * Send command to obd device to retrieve vin from the currently
     * connected car.
     *
     * @see #getParameterData(ParameterPackageInfo) for info returned
     * on the vin query.
     */
    public void getVinFromCar() {
        Log.i(TAG, "Calling getCarVIN from Bluetooth auto-connect");
        bluetoothCommunicator.obdGetParameter(ObdManager.VIN_TAG);
    }


    /**
     * Send command to obd device to retrieve the current device time.
     *
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
     *
     * @see #setParameterResponse(ResponsePackageInfo)
     */
    public void syncObdDevice() {
        Log.i(TAG, "Resetting RTC time - BluetoothAutoConn");

        long systemTime = System.currentTimeMillis();
        bluetoothCommunicator
                .obdSetParameter(ObdManager.RTC_TAG, String.valueOf(systemTime / 1000));
    }

    public void resetObdDeviceTime() {
        Log.i(TAG, "Setting RTC time to 200x - BluetoothAutoConn");

        bluetoothCommunicator
                .obdSetParameter(ObdManager.RTC_TAG, String.valueOf(1088804101));
    }

    public void setFixedUpload() { // to make result 4 pids send every 10 seconds
        Log.i(TAG, "Setting fixed upload parameters");
        bluetoothCommunicator.obdSetParameter(ObdManager.FIXED_UPLOAD_TAG,
                "01;01;01;10;2;2105,2106,2107,210c,210d,210e,210f,2110,2124,2142");
    }

    public void setParam(String tag, String values) {
        Log.i(TAG, "Setting param with tag: " + tag + ", values: " + values);

        bluetoothCommunicator.obdSetParameter(tag, values);
    }

    public void resetDeviceToFactory() {
        Log.i(TAG, "Resetting device to factory settings");

        bluetoothCommunicator.obdSetCtrl(4);
    }

    public void clearObdDataPackage(){
        Log.i(TAG, "Clearing obd data package");

        bluetoothCommunicator.obdSetCtrl(2);
    }

    public int getState() {
        return bluetoothCommunicator.getState();
    }

    public void getRTC(){
        Log.i(TAG, "getting RTC - auto-connect service");
        bluetoothCommunicator.obdGetParameter(ObdManager.RTC_TAG);
    }

    public void getPIDs() { // supported pids
        Log.i(TAG, "getting PIDs - auto-connect service");
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

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() { // for both bluetooth and internet
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {  // device pairing listener
                Log.i(TAG, "Bond state changed: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                    startBluetoothSearch(5);  // start search after pairing in case it disconnects after pair
                }
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {  // bluetooth adapter state listener
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i(TAG, "Bluetooth adapter state changed: " + state);
                bluetoothCommunicator.bluetoothStateChanged(state);
                if (state == BluetoothAdapter.STATE_OFF) {
                    bluetoothCommunicator.close();
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(notifID);
                } else if (state == BluetoothAdapter.STATE_ON && BluetoothAdapter.getDefaultAdapter() != null) {
                    if (bluetoothCommunicator != null) {
                        bluetoothCommunicator.close();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        bluetoothCommunicator = new BluetoothTestAppComm(BluetoothAutoConnectService.this);
                    } else {
                        bluetoothCommunicator = new BluetoothTestAppComm(BluetoothAutoConnectService.this);
                    }

                    bluetoothCommunicator.setBluetoothDataListener(BluetoothAutoConnectService.this);
                    if (BluetoothAdapter.getDefaultAdapter() != null
                            && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startBluetoothSearch(6); // start search when turning bluetooth on
                    }
                }
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) { // internet connectivity listener
                if (NetworkHelper.isConnected(BluetoothAutoConnectService.this)) {
                    Log.i(TAG, "Sending stored PIDS and DTCS");
                    for (final DtcPayload dtcPayload : dtcsToSend) {
                        mNetworkHelper.postTestDtcs(dtcPayload.getPidArr(), dtcPayload.getDtc(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                Log.d(TAG, "DTC posted: " + dtcPayload.getDtc());
                            }
                        });
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

    public void updateState(State state){
        this.state = state;
    }

    public void reset(){
        vinAttempts = 0;
        rtcAttempts = 0;
        rtcSuccess = false;
        vinSuccess = false;
        pidSuccess = false;
        dtcSuccess = false;
        dataSuccess = false;
        mPidSnapshots = new ArrayList<>();
        dtcsToSend = new ArrayList<>();
        state = State.NONE;
    }
}

package com.castel.obd.bluetooth;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.Utils;
import com.castel.obd215b.info.DTCInfo;
import com.castel.obd215b.info.IDRInfo;
import com.castel.obd215b.info.PIDInfo;
import com.castel.obd215b.info.SettingInfo;
import com.castel.obd215b.util.Constants;
import com.castel.obd215b.util.DataPackageUtil;
import com.castel.obd215b.util.DataParseUtil;
import com.castel.obd215b.util.DateUtil;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */


/**
 * Manage LE connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class Bluetooth215BComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager mObdManager;
    private final LinkedList<WriteCommand> mCommandQueue = new LinkedList<>();
    //Command Operation executor - will only run one at a time
    ExecutorService mCommandExecutor = Executors.newSingleThreadExecutor();
    //Semaphore lock to coordinate command executions, to ensure only one is
    //currently started and waiting on a response.
    Semaphore mCommandLock = new Semaphore(1,true);

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 12000;
    //private BluetoothLeScanner mLEScanner;
    //private ScanSettings settings;
    //private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private boolean mIsScanning = false;
    private boolean hasDiscoveredServices = false;

    private static String TAG = "BleCommDebug";

    private boolean needToScan = true; // need to scan after restarting bluetooth adapter even if mGatt != null

    public static final UUID OBD_IDD_212_MAIN_SERVICE =
            //UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); // 212B
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_READ_CHAR =
            //UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"); // 212B
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_WRITE_CHAR =
            //UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"); // 212B
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // 212B

    private int btConnectionState = DISCONNECTED;

    public Bluetooth215BComm(Context context) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        //settings = new ScanSettings.Builder()
        //        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //        .build();
//
        //filters = new ArrayList<>();
        //ParcelUuid serviceUuid = ParcelUuid.fromString(OBD_IDD_212_MAIN_SERVICE.toString());
        //filters.add(new ScanFilter.Builder().setServiceUuid(serviceUuid).build());

        mObdManager = new ObdManager(context);
        //int initSuccess = mObdManager.initializeObd();
        //Log.d(TAG, "init result: " + initSuccess);
    }

    @Override
    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
        mObdManager.setDataListener(this.dataListener);
        mObdManager.setPassiveCommandListener(this);
    }

    @Override
    public boolean hasDiscoveredServices() {
        return hasDiscoveredServices;
    }

    @Override
    public void startScan() {
        if(!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Scan unable to start");
            return;
        }

        if(mIsScanning) {
            Log.i(TAG, "Already scanning");
            return;
        }

        connectBluetooth();
    }

    @Override
    public void stopScan() {
        scanLeDevice(false);
    }

    @Override
    public int getState() {
        return btConnectionState;
    }

    @Override
    public void obdSetCtrl(int type) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = mObdManager.obdSetCtrl(type);
        Log.i(TAG, "Set ctrl result: " + payload);
        writeToObd(payload, 1);
    }

    @Override
    public void obdSetMonitor(int type, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        //String payload = mObdManager.obdSetMonitor(type, valueList);
        String payload = DataPackageUtil.dtcPackage("0", "0");
        writeToObd(payload, 1);
    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = DataPackageUtil.setParameter(tlvTagList, valueList);
        writeToObd(payload, 1);
    }

    @Override
    public void obdGetParameter(String tlvTag) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = DataPackageUtil.getParameter(tlvTag);
        writeToObd(payload, 1);
    }

    @Override
    public void sendCommandPassive(String payload) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(payload, -1);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @Override
    public void close() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }


    /**
     *
     *
     */
    private void writeToObd(String payload, int type) {
        if(!hasDiscoveredServices) {
            return;
        }

        if (type == 0) { // get instruction string from json payload
            try {
                String temp = new JSONObject(payload).getString("instruction");
                payload = temp;
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> sendData = new ArrayList<>(payload.length() % 20 + 1);

        while(payload.length() > 20) {
            sendData.add(payload.substring(0, 20));
            payload = payload.substring(20);
        }
        sendData.add(payload);

        for(String data : sendData) {
            byte[] bytes;

            bytes = mObdManager.getBytesToSendPassive(data);

            if (bytes == null) {
                return;
            }

            Log.i(TAG, "btConnstate: " + btConnectionState + " mGatt value: " + (mGatt != null));
            if (btConnectionState == CONNECTED && mGatt != null) {
                try {
                    queueCommand(new WriteCommand(data.getBytes("UTF-8"), WriteCommand.WRITE_TYPE.DATA)); // TODO: different bytes depending on device
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void queueCommand(WriteCommand command) {
        synchronized (mCommandQueue) {
            Log.i(TAG,"Queue command");
            mCommandQueue.add(command);
            //Schedule a new runnable to process that command (one command at a time executed only)
            ExecuteCommandRunnable runnable = new ExecuteCommandRunnable(command, mGatt);
            mCommandExecutor.execute(runnable);
        }
    }


    //Remove the current command from the queue, and release the lock
    //signalling the next queued command (if any) that it can start
    protected void dequeueCommand(){
        Log.i(TAG, "dequeue command");
        mCommandQueue.pop();
        mCommandLock.release();
    }

    private void showConnectingNotification() {
        Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
                R.mipmap.ic_push);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                        .setProgress(100, 100, true)
                        .setContentTitle("Connecting to car");

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.putExtra(MainActivity.FROM_NOTIF, true);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(BluetoothAutoConnectService.notifID, mBuilder.build());
    }

    /**
     * @param device
     */
    @SuppressLint("NewApi")
    public void connectToDevice(final BluetoothDevice device) {
        if(btConnectionState == CONNECTING) {
            return;
        }
        btConnectionState = CONNECTING;
        Log.i(TAG, "Connecting to device");
        scanLeDevice(false);// will stop after first device detection
        showConnectingNotification();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    mGatt = device.connectGatt(mContext, true, gattCallback);
                }
            }
        });
    }

    public void bluetoothStateChanged(int state) {
        if(state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = DISCONNECTED;
        }
    }

    /**
     *
     *
     */
    private void connectBluetooth() {

        if(btConnectionState == CONNECTED) {
            Log.i(TAG, "Bluetooth connected");
            return;
        }

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

        //scanLeDevice(true);

        if (mGatt != null && !needToScan) {
            try {
                //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
                // Previously connected device.  Try to reconnect.
                if (mGatt.connect()) {
                    Log.i(TAG, "Trying to connect to device - BluetoothLeComm");
                    btConnectionState = CONNECTING;
                } else {
                    //mGatt = null;
                    Log.i(TAG, "Could not connect to previous device, scanning...");
                    scanLeDevice(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown by connect");
                e.printStackTrace();
                mGatt.close();
                mGatt = null;
                scanLeDevice(true);
            }
        } else  {
            Log.i(TAG, "mGatt is null or bluetooth adapter reset");
            scanLeDevice(true);
        }
    }


    /**
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD);

            Log.i(TAG, "Starting le scan");
            mBluetoothAdapter.startLeScan(mScanCallback);
            mIsScanning = true;
        } else {
            Log.i(TAG, "Stopping scan");
            mBluetoothAdapter.stopLeScan(mScanCallback);
            mIsScanning = false;
        }

    }



    /**
     *
     *
     *
     */
    private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device.getName() != null) {
                Log.v(TAG, "Found device: "+device.getName());
                if(device.getName().contains(ObdManager.BT_DEVICE_NAME_212) || device.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
                    connectToDevice(device);
                }
            }
        }
    };


    /**
     *
     *
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange Status: " + status + " new State " + newState);

            switch (newState) {

                case BluetoothProfile.STATE_CONNECTING:
                {
                    Log.i(TAG, "gattCallback STATE_CONNECTING");
                    btConnectionState = CONNECTING;
                    break;
                }

                case BluetoothProfile.STATE_CONNECTED:
                {
                    Log.i(TAG, "ACTION_GATT_CONNECTED");
                    try {
                        mixpanelHelper.trackConnectionStatus(MixpanelHelper.CONNECTED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    needToScan = false;
                    dataListener.getBluetoothState(btConnectionState);
                    btConnectionState = CONNECTED;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            gatt.discoverServices();
                        }
                    }, 500);

                    break;
                }

                case BluetoothProfile.STATE_DISCONNECTING:
                {
                    Log.i(TAG, "gattCallback STATE_DISCONNECTING");
                    break;
                }

                case BluetoothProfile.STATE_DISCONNECTED:
                {
                    Log.i(TAG, "ACTION_GATT_DISCONNECTED");
                    btConnectionState = DISCONNECTED;
                    try {
                        mixpanelHelper.trackConnectionStatus(MixpanelHelper.DISCONNECTED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dataListener.getBluetoothState(btConnectionState);
                    NotificationManager mNotificationManager =
                            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(BluetoothAutoConnectService.notifID);
                    break;
                }

                default:
                    Log.i(TAG, "gattCallback STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                queueCommand(new WriteCommand(null, WriteCommand.WRITE_TYPE.NOTIFICATION));
                hasDiscoveredServices = true;

                // Setting bluetooth state as connected because, you can't communicate with
                // device until services have been discovered
                //btConnectionState = CONNECTED;
                dataListener.getBluetoothState(btConnectionState);

            } else {
                Log.i(TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            if (OBD_READ_CHAR.equals(characteristic.getUuid())) {

                final byte[] data = characteristic.getValue();
                String readData = "";

                try {
                    readData = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Data Read: " + readData);

                if(readData.isEmpty()) {
                    return;
                }

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    parseReadData(readData);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (OBD_READ_CHAR.equals(characteristic.getUuid())) {

                final byte[] data = characteristic.getValue();

                String readData = "";

                try {
                    readData = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Data Read: " + readData);

                if(readData.isEmpty()) {
                    return;
                }

                final String dataToParse = readData;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        parseReadData(dataToParse);
                    }
                });
            }


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "OnCharacteristicWrite " + status);
            dequeueCommand();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "Descriptor written");
            dequeueCommand();
        }
    };

    private StringBuilder sbRead = new StringBuilder();

    private void parseReadData(String msg) {
        sbRead.append(msg);

        if (sbRead.toString().contains("\r\n")) {
            String msgInfo = sbRead.toString().replace("\r\n",
                    "\\r\\n");

            sbRead = new StringBuilder();
            if (Constants.INSTRUCTION_IDR.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {

                String dateStr = DateUtil.getSystemTime("yyyy-MM-dd HH:mm:ss");

                IDRInfo idrInfo = DataParseUtil.parseIDR(msgInfo);
                idrInfo.time = dateStr;

                //Log.i(TAG, idrInfo.toString());

            } else if (Constants.INSTRUCTION_CI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //boolean bResult = DataParseUtil
                //        .parseSetting(msgInfo);
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_CI);
                //intent.putExtra(EXTRA_DATA, bResult);
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);
//
                ////
                //broadcastContent(ACTION_COMMAND_TEST,
                //        COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
            } else if (Constants.INSTRUCTION_SI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                boolean bResult = DataParseUtil
                        .parseSetting(msgInfo);

                Log.i(TAG, "SI result: " + bResult);

                //dataListener.setParameterResponse();
            } else if (Constants.INSTRUCTION_QI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                SettingInfo settingInfo = DataParseUtil
                        .parseQI(msgInfo);

                ParameterPackageInfo parameterPackageInfo = new ParameterPackageInfo();
                parameterPackageInfo.deviceId = settingInfo.terminalSN;
                parameterPackageInfo.result = 1;
                parameterPackageInfo.value = new ArrayList<>();

                if(settingInfo.terminalRTCTime != null) {
                    parameterPackageInfo.value.add(new ParameterInfo(DataPackageUtil.RTC_TIME_PARAM, settingInfo.terminalRTCTime));
                }
                if(settingInfo.vehicleVINCode != null) {
                    parameterPackageInfo.value.add(new ParameterInfo(ObdManager.VIN_TAG, settingInfo.vehicleVINCode));
                }

                dataListener.getParameterData(parameterPackageInfo);
            } else if (Constants.INSTRUCTION_PIDT
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                // PIDInfo pidInfo = DataParseUtil.parsePIDT(msgInfo);
//
                // intent.putExtra(EXTRA_DATA_TYPE,
                //         Constants.INSTRUCTION_PIDT);
                // intent.putExtra(EXTRA_DATA, pidInfo);
                // LocalBroadcastManager.getInstance(this)
                //         .sendBroadcast(intent);
//
                // broadcastContent(ACTION_COMMAND_TEST,
                //         COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
            } else if (Constants.INSTRUCTION_PID
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //PIDInfo pidInfo = DataParseUtil.parsePID(msgInfo);
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_PID);
                //intent.putExtra(EXTRA_DATA, pidInfo);
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);
//
                //broadcastContent(ACTION_COMMAND_TEST,
                //        COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
            } else if (Constants.INSTRUCTION_DTC
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                Log.i(TAG, msgInfo);
                DTCInfo dtcInfo = DataParseUtil.parseDTC(msgInfo);

                Log.i(TAG, dtcInfo.toString());

                DataPackageInfo dataPackageInfo = new DataPackageInfo();

                dataPackageInfo.result = 6;
                StringBuilder sb = new StringBuilder();
                if(dtcInfo.dtcs != null) {
                    for (String dtc : dtcInfo.dtcs) {
                        sb.append(dtc.substring(4));
                        sb.append(",");
                    }
                }
                if(sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                dataPackageInfo.dtcData = sb.toString();
                dataPackageInfo.deviceId = dtcInfo.terminalId;
                dataPackageInfo.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
                dataListener.getIOData(dataPackageInfo);
            } else if (Constants.INSTRUCTION_OTA
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //LogUtil.i("--????OTA????--:" + msgInfo);
//
                //// ???????
                //mRecieveTerminate = System.currentTimeMillis();
                //LogUtil.v("send next data2");
                //broadcastUpdateContent(ACTION_PACKAGE_CONTENT,
                //        "\n"+getResources().getString(R.string.report_data) + msgInfo+"\n"
                //                + DateUtil.getSystemTime()+"\n");
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_OTA);
                //intent.putExtra(EXTRA_DATA,
                //        DataParseUtil.parseOTA(msgInfo));
                //intent.putExtra(EXTRA_DATA1, DataParseUtil.parseUpgradeType(msgInfo));
//
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);

            } else if (Constants.INSTRUCTION_TEST
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //LogUtil.i("--??????--:" + msgInfo);
                //broadcastContent(ACTION_HARDWARE_TEST,
                //        HARDWARE_TEST_DATA, "\n"+getResources().getString(R.string.report_data) + msgInfo);
            }

            if (!Constants.INSTRUCTION_TEST.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {
            }
        }
    }

    //Runnable to execute a command from the queue
    class ExecuteCommandRunnable implements Runnable{

        private WriteCommand mCommand;
        private BluetoothGatt mGatt;

        public ExecuteCommandRunnable(WriteCommand command, BluetoothGatt gatt) {
            mCommand = command;
            mGatt = gatt;

        }

        @Override
        public void run() {
            //Acquire semaphore lock to ensure no other operations can run until this one completed
            mCommandLock.acquireUninterruptibly();
            //Tell the command to start itself.
            //Log.d(TAG, "WriteCommand: " + Utils.bytesToHexString(mCommand.bytes));
            mCommand.execute(mGatt);
        }
    }

    @Override
    public void writeRawInstruction(String instruction) {
        sendCommandPassive(instruction);
    }

    @Override
    public void initDevice() {
        mObdManager.initializeObd();
    }
}

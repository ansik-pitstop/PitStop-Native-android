package com.pitstop.bluetooth;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.castel.obd.bleDevice.AbstractDevice;
import com.castel.obd.bleDevice.Device212B;
import com.castel.obd.bleDevice.Device215B;
import com.castel.obd.bluetooth.BluetoothClassicComm;
import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.BluetoothLeComm;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd215b.util.DataPackageUtil;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ben!
 */
public class BluetoothDeviceManager implements ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;

    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 12000;
    private boolean hasDiscoveredServices = false;

    private Handler mHandler = new Handler();

    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private boolean needToScan = true; // need to scan after restarting bluetooth adapter even if mGatt != null

    public static final UUID OBD_IDD_212_MAIN_SERVICE =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); // 212B
    public static final UUID OBD_READ_CHAR_212 =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"); // 212B
    public static final UUID OBD_WRITE_CHAR_212 =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"); // 212B

    public static final UUID OBD_IDD_215_MAIN_SERVICE =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_READ_CHAR_215 =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_WRITE_CHAR_215 =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // 215B

    public static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int btConnectionState = BluetoothCommunicator.DISCONNECTED;

    private AbstractDevice deviceInterface;

    private BluetoothCommunicator communicator;

    private final BluetoothDeviceRecognizer mBluetoothDeviceRecognizer;

    private BluetoothDevice connectedDevice;

    //List of invalid addresses from current or past search that we do not want to deal with again
    private List<BluetoothDevice> bannedDeviceList = new ArrayList<>();

    public enum CommType {
        CLASSIC, LE
    }

    public BluetoothDeviceManager(Context context) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // for classic discovery
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, intentFilter);

        mBluetoothDeviceRecognizer = new BluetoothDeviceRecognizer(context);
    }

    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void startScan() {
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Scan unable to start");
            mBluetoothAdapter.enable();
            return;
        }

        if (mBluetoothAdapter.isDiscovering()) {
            Log.i(TAG, "Already scanning");
            return;
        }

        connectBluetooth();
    }

    @Override
    public void sendCommandPassive(String payload) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(payload);
    }

    public void close() {
        if (communicator != null) {
            communicator.close();
        }
        try {
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
    }

    private void writeToObd(String payload) {
        if (communicator == null) {
            return;
        }

        if (payload == null || payload.isEmpty()) {
            return;
        }

        try { // get instruction string from json payload
            String temp = new JSONObject(payload).getString("instruction");
            payload = temp;
        } catch (JSONException e) {
        }

        ArrayList<String> sendData = new ArrayList<>(payload.length() % 20 + 1);

        while (payload.length() > 20) {
            sendData.add(payload.substring(0, 20));
            payload = payload.substring(20);
        }
        sendData.add(payload);

        for (String data : sendData) {
            byte[] bytes;

            bytes = deviceInterface.getBytes(data);

            if (bytes == null || bytes.length == 0) {
                return;
            }

            communicator.writeData(bytes);
        }
    }

    public void connectionStateChange(int state) {
        btConnectionState = state;
        dataListener.getBluetoothState(state);

        // on device connected?
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

    public void onConnectedDeviceInvalid(){
        bannedDeviceList.add(connectedDevice);
        communicator.disconnect(connectedDevice);
        startScan();
    }

    /**
     * @param device
     */
    @SuppressLint("NewApi")
    public void connectToDevice(final BluetoothDevice device) {
        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            return;
        }

        // Le can be used for 212 but it doesn't work properly on all versions of Android
        // scanLeDevice(false);// will stop after first device detection

        if (communicator != null) {
            communicator.close();
            communicator = null;
        }

        switch (deviceInterface.commType()) {
            case LE:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                Log.i(TAG, "Connecting to LE device");
                showConnectingNotification();
                communicator = new BluetoothLeComm(mContext, this, deviceInterface.getServiceUuid(),
                        deviceInterface.getWriteChar(), deviceInterface.getReadChar());
                break;
            case CLASSIC:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                Log.i(TAG, "Connecting to Classic device");
                communicator = new BluetoothClassicComm(mContext, this);
                showConnectingNotification();
                break;
        }

        connectedDevice = device;
        communicator.connectToDevice(device);
    }

    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
        }
    }

    public int getState() {
        return btConnectionState;
    }

    public String getConnectedDeviceName() {
        return deviceInterface.getDeviceName();
    }

    private void connectBluetooth() {
        btConnectionState = communicator == null ? BluetoothCommunicator.DISCONNECTED : communicator.getState();

        if (btConnectionState == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Bluetooth connected");
            return;
        }

        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting");
            return;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

        Log.i(TAG, "BluetoothAdapter starts discovery");
        mBluetoothAdapter.startDiscovery();

        //scanLeDevice(true);

        //if (mGatt != null && !needToScan) {
        //    try {
        //        //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
        //        // Previously connected device.  Try to reconnect.
        //        if (mGatt.connect()) {
        //            Log.i(TAG, "Trying to connect to device - BluetoothLeComm");
        //            btConnectionState = CONNECTING;
        //        } else {
        //            //mGatt = null;
        //            Log.i(TAG, "Could not connect to previous device, scanning...");
        //            scanLeDevice(true);
        //        }
        //    } catch (Exception e) {
        //        Log.e(TAG, "Exception thrown by connect");
        //        e.printStackTrace();
        //        mGatt.close();
        //        mGatt = null;
        //        scanLeDevice(true);
        //    }
        //} else  {
        //    Log.i(TAG, "mGatt is null or bluetooth adapter reset");
        //    scanLeDevice(true);
        //}
    }

    private boolean isBanned(BluetoothDevice device){
        for (BluetoothDevice d: bannedDeviceList){
            if (d.getAddress().equals(device.getAddress())
                    && d.getName().equals(device.getName())){
                return true;
            }
        }
        return false;
    }

    // for classic discovery
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, BluetoothDevice.ACTION_FOUND);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                Log.d(TAG, deviceName + "   " + device.getAddress());

                //Check whether device has already been added to invalid list during prior search
                if (isBanned(device)){
                    Log.v(TAG, "Device " + deviceName+" with address: "+device.getAddress()
                            +" is on banned list and being ignored");
                    return;
                }

                switch (mBluetoothDeviceRecognizer.onDeviceFound(deviceName)) {
                    case CONNECT:
                        Log.v(TAG, "Found device: " + deviceName);
                        if (deviceName.contains(ObdManager.BT_DEVICE_NAME_212)) {
                            deviceInterface = new Device212B(mContext, dataListener, BluetoothDeviceManager.this, deviceName);
                            connectToDevice(device);
                        } else if (deviceName.contains(ObdManager.BT_DEVICE_NAME_215)) {
                            deviceInterface = new Device215B(mContext, dataListener, deviceName);
                            connectToDevice(device);
                        }
                        break;
                }
            }
        }
    };

    public void readData(final byte[] data) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                deviceInterface.parseData(data);
            }
        });
    }

    // functions

    public void setParam(String param, String value) { // 215B specific
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        if (!(deviceInterface instanceof Device215B)) return;

        Log.d(TAG, DataPackageUtil.siMulti(param, value));
        writeToObd(DataPackageUtil.siMulti(param, value));
    }

    public void getVin() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getVin()); // 212 parser returns json
    }

    public void getRtc() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getRtc());
    }

    public void setDeviceNameAndId(String name){
        //Device name should never be set for 212
        if (isConnectedTo215()){
            Device215B device215B = (Device215B)deviceInterface;
            Log.d(TAG,"Setting device name and id to "+name+", command: "+device215B.setDeviceNameAndId(name));
            writeToObd(device215B.setDeviceNameAndId(name));
        }
    }

    public void setRtc(long rtcTime) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setRtc(rtcTime));
    }

    public void getPids(String pids) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getPids(pids));
    }

    public void getSupportedPids() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getSupportedPids());
    }

    // sets pids to check and sets data interval
    public void setPidsToSend(String pids) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setPidsToSend(pids));
    }

    public void getDtcs() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getDtcs());
        writeToObd(deviceInterface.getPendingDtcs());
    }

    public void getFreezeFrame() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getFreezeFrame());
    }

    public void requestData() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.requestData());
    }

    public void getRtcAndMileage(){
        if (isConnectedTo215()){
            writeToObd(((Device215B) deviceInterface).getRtcAndMileage());
        }
    }

    public boolean isConnectedTo215(){
        if (deviceInterface != null)
            return deviceInterface instanceof Device215B;
        else
            return false;
    }

    public int getConnectionState(){
        return btConnectionState;
    }

}

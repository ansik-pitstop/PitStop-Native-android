package com.castel.obd.bluetooth;

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
import com.castel.obd215b.util.DataPackageUtil;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */


/**
 * Manage LE connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothDeviceManager implements ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;

    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 12000;
    private boolean mIsScanning = false;
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

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, intentFilter);
    }

    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
    }

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
    public void sendCommandPassive(String payload) {
        if(btConnectionState == BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(payload);
    }

    public void close() {
        if(communicator != null) {
            communicator.close();
        }
        try {
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
    }

    private void writeToObd(String payload) {
        if(communicator == null) {
            Log.w(TAG, "Try to write with null communicator");
            return;
        }

        if(payload == null || payload.isEmpty()) {
            Log.w(TAG, "Nothing to write");
            return;
        }

        try { // get instruction string from json payload
            String temp = new JSONObject(payload).getString("instruction");
            payload = temp;
        } catch(JSONException e) {
        }

        ArrayList<String> sendData = new ArrayList<>(payload.length() % 20 + 1);

        while(payload.length() > 20) {
            sendData.add(payload.substring(0, 20));
            payload = payload.substring(20);
        }
        sendData.add(payload);

        for(String data : sendData) {
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
        if(btConnectionState == BluetoothCommunicator.CONNECTING) {
            return;
        }

        scanLeDevice(false);// will stop after first device detection

        if(communicator != null) {
            communicator.close();
            communicator = null;
        }

        switch(deviceInterface.commType()) {
            case LE:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                Log.i(TAG, "Connecting to LE device");
                showConnectingNotification();
                communicator = new BluetoothLeComm(mContext, this, deviceInterface.getServiceUuid(),
                        deviceInterface.getWriteChar(), deviceInterface.getReadChar());
                break;
            case CLASSIC:
                communicator = new BluetoothClassicComm(mContext, this);
                break;
        }

        if(communicator != null) {
            communicator.connectToDevice(device);
        }
    }

    public void bluetoothStateChanged(int state) {
        if(state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
        }
    }

    public int getState() {
        return btConnectionState;
    }

    private void connectBluetooth() {
        btConnectionState = communicator == null ? BluetoothCommunicator.DISCONNECTED : communicator.getState();

        if(btConnectionState == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Bluetooth connected");
            return;
        }

        if(btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting");
            return;
        }

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName() != null) {
                    Log.v(TAG, "Found device: "+device.getName());
                    if(device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                        deviceInterface = new Device212B(mContext, dataListener, BluetoothDeviceManager.this);
                        connectToDevice(device);
                    } else if(device.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
                        deviceInterface = new Device215B(mContext, dataListener);
                        connectToDevice(device);
                    }
                }
            }
        }
    };

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

    private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device.getName() != null) {
                Log.v(TAG, "Found device: "+device.getName());
                if(device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                    deviceInterface = new Device212B(mContext, dataListener, BluetoothDeviceManager.this);
                    connectToDevice(device);
                } else if(device.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
                    deviceInterface = new Device215B(mContext, dataListener);
                    connectToDevice(device);
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

    public void getVin() {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getVin()); // 212 parser returns json
    }

    public void getRtc() {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getRtc());
    }

    public void setRtc(long rtcTime) {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setRtc(rtcTime));
    }

    public void getPids(String pids) {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getPids(pids));
    }

    public void getSupportedPids() {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getSupportedPids());
    }

    // sets pids to check and sets data interval
    public void setPidsToSend(String pids) {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setPidsToSend(pids));
    }

    public void getDtcs() {
        if(btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getDtcs());
    }
}

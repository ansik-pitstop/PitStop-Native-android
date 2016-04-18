package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.Utils;
import com.pitstop.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */


/**
 * Manage LE connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.M)
public class BluetoothLeComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    public final static String ACTION_GATT_CONNECTED =
            "com.pitstop.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.pitstop.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.pitstop.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.pitstop.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.pitstop.bluetooth.le.EXTRA_DATA";

    private Context mContext;
    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager mObdManager;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private boolean mIsScanning = false;

    private BluetoothGattService mainObdGattService;


    private static final UUID OBD_IDD_212_MAIN_SERVICE =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID OBD_READ_CHAR =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID OBD_WRITE_CHAR =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int btConnectionState = DISCONNECTED;

    public BluetoothLeComm(Context context) {

        mContext = context;
        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        filters = new ArrayList<>();
        ParcelUuid serviceUuid = ParcelUuid.fromString(OBD_IDD_212_MAIN_SERVICE.toString());
        filters.add(new ScanFilter.Builder().setServiceUuid(serviceUuid).build());

        mObdManager = new ObdManager(context);
        mObdManager.initializeObd();

        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
        mObdManager.setDataListener(this.dataListener);
        mObdManager.setPassiveCommandListener(this);
    }

    @Override
    public void startScan() {
        if(mLEScanner == null || mIsScanning) {
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
        writeToObd(payload, 0);
    }

    @Override
    public void obdSetMonitor(int type, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = mObdManager.obdSetMonitor(type, valueList);
        writeToObd(payload, 0);
    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = mObdManager.obdSetParameter(tlvTagList, valueList);
        writeToObd(payload, 0);
    }

    @Override
    public void obdGetParameter(String tlvTag) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = mObdManager.obdGetParameter(tlvTag);
        writeToObd(payload, 0);
    }

    @Override
    public void sendCommandPassive(String payload) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(payload, -1);
    }


    /**
     *
     *
     */
    private synchronized void writeToObd(String payload, int type) {

        byte[] bytes;

        if(type == 0) {
            bytes = mObdManager.getBytesToSend(payload);
        } else {
            bytes = mObdManager.getBytesToSendPassive(payload);
        }

        if(bytes == null) {
            return;
        }

        if (btConnectionState == CONNECTED && mGatt != null ) {
            Log.i(MainActivity.TAG, "Writing characteristic...");
            BluetoothGattCharacteristic obdWriteCharacteristic =
                    mainObdGattService.getCharacteristic(OBD_WRITE_CHAR);
            obdWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            obdWriteCharacteristic.setValue(bytes);
            mGatt.writeCharacteristic(obdWriteCharacteristic);
        }
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
        mContext.unregisterReceiver(mGattUpdateReceiver);
        mGatt.close();
        mGatt = null;
    }

    /**
     * @param device
     */
    private void connectToDevice(BluetoothDevice device) {
        if(mGatt == null) {
            mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            mGatt.requestMtu(512);
            scanLeDevice(false);// will stop after first device detection
            btConnectionState = CONNECTING;
        }
    }



    /**
     *
     *
     */
    private void connectBluetooth() {

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()
                || btConnectionState == CONNECTED) {
            Log.i(MainActivity.TAG, "Bluetooth connected or Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

        Log.i(MainActivity.TAG,"Getting saved macAddress - BluetoothLeComm");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        if (macAddress != null && !macAddress.equals("") && mGatt != null) {

            // BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            // Previously connected device.  Try to reconnect.
            if(mGatt.connect()) {
                Log.i(MainActivity.TAG,"Trying to connect to device - BluetoothLeComm");
                btConnectionState = CONNECTING;
            }

        } else {

            Log.i(MainActivity.TAG, "macAddress is null or empty or mGatt is null");
            if(mGatt != null) {

                Log.i(MainActivity.TAG, "closing mGatt connection");
                mGatt.close();
                mGatt = null;
            }

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
                    Log.i(MainActivity.TAG, "Stopping scan");
                    mIsScanning = false;
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            Log.i(MainActivity.TAG, "Starting le scan");
            mLEScanner.startScan(filters, settings, mScanCallback);
            mIsScanning = true;
        } else {
            mLEScanner.stopScan(mScanCallback);
            mIsScanning = false;
        }

    }



    /**
     *
     *
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(MainActivity.TAG, "Result: "+result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if(btDevice.getName() != null && btDevice.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) { }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(MainActivity.TAG, "Scan Failed Error Code: " + errorCode);
        }
    };



    /**
     *
     *
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (ACTION_GATT_CONNECTED.equals(action)) {

                Log.i(MainActivity.TAG, "ACTION_GATT_CONNECTED");
                dataListener.getBluetoothState(btConnectionState);

            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {

                Log.i(MainActivity.TAG, "ACTION_GATT_DISCONNECTED");
                dataListener.getBluetoothState(btConnectionState);

            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

                Log.i(MainActivity.TAG, "ACTION_GATT_SERVICES_DISCOVERED");

            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                mObdManager.receiveDataAndParse(intent.getStringExtra(EXTRA_DATA));
            }
        }
    };

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }



    /**
     *
     *
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        if (OBD_READ_CHAR.equals(characteristic.getUuid())) {

            final Intent intent = new Intent(action);

            final byte[] data = characteristic.getValue();
            final String hexData = Utils.bytesToHexString(data);

            if(Utils.isEmpty(hexData)) {
                return;
            }

            intent.putExtra(EXTRA_DATA, hexData);

            mContext.sendBroadcast(intent);
        } else {
            Log.i(MainActivity.TAG, "Write characteristic received");
        }

    }



    /**
     *
     *
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(MainActivity.TAG, "onConnectionStateChange Status: " + status + " new State " + newState);

            switch (newState) {

                case BluetoothProfile.STATE_CONNECTING:
                {
                    Log.i(MainActivity.TAG, "gattCallback STATE_CONNECTING");
                    break;
                }

                case BluetoothProfile.STATE_CONNECTED:
                {
                    btConnectionState = CONNECTED;
                    BluetoothDevice device = gatt.getDevice();
                    OBDInfoSP.saveMacAddress(mContext, device.getAddress());
                    gatt.discoverServices();
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    break;
                }

                case BluetoothProfile.STATE_DISCONNECTING:
                {
                    Log.i(MainActivity.TAG, "gattCallback STATE_DISCONNECTING");
                    break;
                }

                case BluetoothProfile.STATE_DISCONNECTED:
                {
                    btConnectionState = DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    break;
                }

                default:
                    Log.e(MainActivity.TAG, "gattCallback STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                mainObdGattService = gatt.getService(OBD_IDD_212_MAIN_SERVICE);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                BluetoothGattCharacteristic obdReadCharacteristic =
                        mainObdGattService.getCharacteristic(OBD_READ_CHAR);

                setNotificationOnCharacteristic(obdReadCharacteristic);

            } else {
                Log.i(MainActivity.TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
            }
        }
    };


    /**
     *
     *
     */
    private void setNotificationOnCharacteristic(BluetoothGattCharacteristic characteristic) {

        if(characteristic != null) {
            Log.i(MainActivity.TAG, "Setting notification on: " + characteristic.getUuid());

            // Enable local notification
            mGatt.setCharacteristicNotification(characteristic, true);

            // Enable remote notification
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            Log.i(MainActivity.TAG, "descriptor: " + descriptor.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(descriptor);
        }
    }
}

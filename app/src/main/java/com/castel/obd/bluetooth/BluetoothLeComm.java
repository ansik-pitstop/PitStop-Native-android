package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.util.Log;

import com.pitstop.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
@TargetApi(Build.VERSION_CODES.M)
public class BluetoothLeComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

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

    private int btConnectionState = DISCONNECTED;

    public BluetoothLeComm(Context context) {
        Log.i(MainActivity.TAG, "BleComm Constructor");

        mContext = context;
        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();
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
        if(mLEScanner == null) {
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
        return 0;
    }

    @Override
    public void obdSetCtrl(int type) {

    }

    @Override
    public void obdSetMonitor(int type, String valueList) {

    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {

    }

    @Override
    public void obdGetParameter(String tlvTag) {

    }

    @Override
    public void sendCommandPassive(String instruction) {

    }

    @Override
    public void close() {
        if (mGatt == null) {
            return;
        }
        mContext.unregisterReceiver(mGattUpdateReceiver);
        mGatt.close();
        mGatt = null;
    }

    private void connectBluetooth() {
        scanLeDevice(true);

        /*Log.i(MainActivity.TAG,"Getting saved macAddress - BluetoothManage");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        if(mBluetoothAdapter == null || btConnectionState == CONNECTED) {
            Log.i(MainActivity.TAG,"Bluetooth is connected - BluetoothClassicComm");
            return;
        }

        // TODO
        if(!mBluetoothAdapter.isEnabled()) {
            Log.i(MainActivity.TAG,"Bluetooth not enabled");
            mBluetoothAdapter.enable();
        }

        if (macAddress != null && !macAddress.equals("") && mGatt != null) {
            Log.i(MainActivity.TAG,"Using macAddress "+macAddress+" to connect to device - BluetoothManage");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            if(mGatt.connect()) {
                btConnectionState = CONNECTING;
            }
        } else {
            mGatt = null;
            scanLeDevice(true);
            btConnectionState = CONNECTING;
        }*/
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
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            Log.i(MainActivity.TAG, "Starting le scan");
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(MainActivity.TAG, "callbackType: "+String.valueOf(callbackType));
            Log.i(MainActivity.TAG, "Result: "+result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if(btDevice.getName() != null && btDevice.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i(MainActivity.TAG, "ScanResult - Results: "+sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(MainActivity.TAG, "Scan Failed Error Code: " + errorCode);
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(MainActivity.TAG, "Received broadcast");
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(MainActivity.TAG, "ACTION_GATT_CONNECTED");
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(MainActivity.TAG, "ACTION_GATT_DISCONNECTED");
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

                Log.i(MainActivity.TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(MainActivity.TAG, "ACTION_DATA_AVAILABLE");
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

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(MainActivity.TAG, "onConnectionStateChange Status: " + status + " new State " + newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(MainActivity.TAG, "gattCallback STATE_CONNECTED");
                    btConnectionState = CONNECTED;
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(MainActivity.TAG, "gattCallback STATE_DISCONNECTED");
                    btConnectionState = DISCONNECTED;
                    break;
                default:
                    Log.e(MainActivity.TAG, "gattCallback STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();

            for(BluetoothGattService service : services) {
                Log.i(MainActivity.TAG, "onServicesDiscovered: "+service.getUuid());

                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    gatt.readCharacteristic(characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {

            Log.i(MainActivity.TAG, "onCharacteristicRead: "+characteristic.getUuid());
            Log.i(MainActivity.TAG, "onCharacteristicRead service uuid: "+characteristic.getService().getUuid());
        }
    };


    /**
     * @param device
     */
    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            scanLeDevice(false);// will stop after first device detection
        }
    }
}

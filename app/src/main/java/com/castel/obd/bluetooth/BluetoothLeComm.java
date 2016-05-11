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
import com.pitstop.parse.ParseApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */


/**
 * Manage LE connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.M)
public class BluetoothLeComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    private Context mContext;
    private ParseApplication application;
    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager mObdManager;
    private final LinkedList<BluetoothCommand> mCommandQueue = new LinkedList<>();
    //Command Operation executor - will only run one at a time
    Executor mCommandExecutor = Executors.newSingleThreadExecutor();
    //Semaphore lock to coordinate command executions, to ensure only one is
    //currently started and waiting on a response.
    Semaphore mCommandLock = new Semaphore(1,true);

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 12000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private boolean mIsScanning = false;
    private boolean hasDiscoveredServices = false;

    private static String TAG = "BleCommDebug";


    public static final UUID OBD_IDD_212_MAIN_SERVICE =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID OBD_READ_CHAR =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public static final UUID OBD_WRITE_CHAR =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    public static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int btConnectionState = DISCONNECTED;

    public BluetoothLeComm(Context context) {

        mContext = context;
        application = (ParseApplication) context.getApplicationContext();
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
        if(!mBluetoothAdapter.isEnabled() || mLEScanner == null || mIsScanning) {
            Log.i(TAG, "Scan unable to start");
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

        byte[] bytes;

        if(type == 0) {
            bytes = mObdManager.getBytesToSend(payload);
        } else {
            bytes = mObdManager.getBytesToSendPassive(payload);
        }

        if(bytes == null) {
            return;
        }

        Log.i(TAG, "btConnstate: "+btConnectionState + " mGatt value: "+ (mGatt!=null));
        if (btConnectionState == CONNECTED && mGatt != null ) {
            queueCommand(new WriteCommand(bytes, WriteCommand.WRITE_TYPE.DATA));
        }
    }


    private void queueCommand(BluetoothCommand command) {
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
            Log.i(TAG, "Bluetooth connected or Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

        Log.i(TAG,"Getting saved macAddress - BluetoothLeComm");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        if (mGatt != null) {

            // BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            // Previously connected device.  Try to reconnect.
            if(mGatt.connect()) {
                Log.i(TAG,"Trying to connect to device - BluetoothLeComm");
                btConnectionState = CONNECTING;
            } else {
                Log.i(TAG,"Could not connect to preivous device, scanning...");
                scanLeDevice(true);
            }

        } else  {

            Log.i(TAG, "mGatt is null");
            Log.i(TAG, "closing mGatt connection");

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
                    Log.i(TAG, "Stopping scan");
                    mIsScanning = false;
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            Log.i(TAG, "Starting le scan");
            mLEScanner.startScan(new ArrayList<ScanFilter>(), settings, mScanCallback);
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
            BluetoothDevice btDevice = result.getDevice();
            Log.i(TAG, "Result: "+result.toString());

            if(btDevice.getName() != null
                    && btDevice.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) { }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "Scan Failed Error Code: " + errorCode);
        }
    };


    /**
     *
     *
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
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
                        application.getMixpanelAPI().track("Peripheral Connection Status",
                                new JSONObject("{'Status':'App is connected to bluetooth device (BLE)'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    gatt.discoverServices();
                    BluetoothDevice device = gatt.getDevice();
                    OBDInfoSP.saveMacAddress(mContext, device.getAddress());
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
                        application.getMixpanelAPI().track("Peripheral Connection Status",
                                new JSONObject("{'Status':'App disconnected from bluetooth device (BLE)'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dataListener.getBluetoothState(btConnectionState);
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
                btConnectionState = CONNECTED;
                dataListener.getBluetoothState(btConnectionState);

            } else {
                Log.i(TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (OBD_READ_CHAR.equals(characteristic.getUuid())) {

                final byte[] data = characteristic.getValue();
                final String hexData = Utils.bytesToHexString(data);

                if(Utils.isEmpty(hexData)) {
                    return;
                }

                mObdManager.receiveDataAndParse(hexData);
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

    //Runnable to execute a command from the queue
    class ExecuteCommandRunnable implements Runnable{

        private BluetoothCommand mCommand;
        private BluetoothGatt mGatt;

        public ExecuteCommandRunnable(BluetoothCommand command, BluetoothGatt gatt) {
            mCommand = command;
            mGatt = gatt;

        }

        @Override
        public void run() {
            //Acquire semaphore lock to ensure no other operations can run until this one completed
            mCommandLock.acquireUninterruptibly();
            //Tell the command to start itself.
            mCommand.execute(mGatt);
        }
    };
}

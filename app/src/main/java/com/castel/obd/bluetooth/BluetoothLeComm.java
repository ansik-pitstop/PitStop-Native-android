package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.Utils;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
@TargetApi(Build.VERSION_CODES.M)
public class BluetoothLeComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager mObdManager;
    private final LinkedList<BluetoothCommand> mCommandQueue = new LinkedList<>();
    //Command Operation executor - will only run one at a time
    ExecutorService mCommandExecutor = Executors.newSingleThreadExecutor();
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

    private boolean needToScan = true; // need to scan after restarting bluetooth adapter even if mGatt != null

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
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

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
        if(!mBluetoothAdapter.isEnabled() || mLEScanner == null) {
            Log.i(TAG, "Scan unable to start");
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
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
    public void connectToDevice(BluetoothDevice device) {
        //if(device.getBondState() == BluetoothDevice.BOND_NONE) {
        //    Log.i(TAG, "Bonding to device");
        //    device.createBond();
        //}
        if(mGatt == null) {
            Log.i(TAG, "Connecting to device");
            scanLeDevice(false);// will stop after first device detection
            showConnectingNotification();
            mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            btConnectionState = CONNECTING;
            boolean mtuSuccess = mGatt.requestMtu(512);
            Log.i(TAG, "mtu request " + (mtuSuccess ? "success" : "failed"));
        }
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

        //Log.i(TAG,"Getting saved macAddress - BluetoothLeComm");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        //scanLeDevice(true);

        if (mGatt != null && !needToScan) {
            try {
                // BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
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
            mLEScanner.startScan(new ArrayList<ScanFilter>(), settings, mScanCallback);
            mIsScanning = true;
        } else {
            Log.i(TAG, "Stopping scan");
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
            Log.v(TAG, "Result: "+result.toString());

            if(btDevice.getName() != null
                    && btDevice.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        }

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
                        mixpanelHelper.trackConnectionStatus(MixpanelHelper.CONNECTED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    needToScan = false;

                    btConnectionState = CONNECTED;
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
    }
}

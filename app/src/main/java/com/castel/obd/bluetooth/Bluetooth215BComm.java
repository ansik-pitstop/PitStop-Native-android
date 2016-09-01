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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.bleDevice.AbstractDevice;
import com.castel.obd.bleDevice.Device212B;
import com.castel.obd.bleDevice.Device215B;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.Utils;
import com.castel.obd215b.util.DataPackageUtil;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

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
public class Bluetooth215BComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;
    private final LinkedList<WriteCommand> mCommandQueue = new LinkedList<>();
    //Command Operation executor - will only run one at a time
    ExecutorService mCommandExecutor = Executors.newSingleThreadExecutor();
    //Semaphore lock to coordinate command executions, to ensure only one is
    //currently started and waiting on a response.
    Semaphore mCommandLock = new Semaphore(1,true);

    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 12000;
    private BluetoothGatt mGatt; // for ble communication
    private boolean mIsScanning = false;
    private boolean hasDiscoveredServices = false;

    private List<String> dataLists = new ArrayList<String>();
    private ObdManager mObdManager;
    private BluetoothChat mBluetoothChat; // for bt classic communication

    private static final String TAG = Bluetooth215BComm.class.getSimpleName();

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

    private int btConnectionState = DISCONNECTED;

    private AbstractDevice deviceInterface;

    public enum CommType {
        CLASSIC, LE
    }

    public Bluetooth215BComm(Context context) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        mObdManager = new ObdManager(context);
        mBluetoothChat = new BluetoothChat(mHandler);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
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

        String payload = ObdManager.obdSetCtrl(type);
        Log.i(TAG, "Set ctrl result: " + payload);
        writeToObd(payload);
    }

    @Override
    public void obdSetMonitor(int type, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        //String payload = mObdManager.obdSetMonitor(type, valueList);
        String payload = DataPackageUtil.dtcPackage("0", "0");
        writeToObd(payload);
    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = DataPackageUtil.siSingle(tlvTagList, valueList);
        writeToObd(payload);
    }

    @Override
    public void obdGetParameter(String tlvTag) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String payload = DataPackageUtil.qiSingle(tlvTag);
        writeToObd(payload);
    }

    @Override
    public void sendCommandPassive(String payload) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(payload);
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
    private void writeToObd(String payload) {
        if(!hasDiscoveredServices) {
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

            Log.i(TAG, "btConnstate: " + btConnectionState + " mGatt value: " + (mGatt != null));
            if (btConnectionState == CONNECTED && mGatt != null) {
                queueCommand(new WriteCommand(bytes, WriteCommand.WRITE_TYPE.DATA, deviceInterface.getServiceUuid(),
                        deviceInterface.getWriteChar(), deviceInterface.getReadChar()));
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

        switch(deviceInterface.commType()) {
            case LE:
                mCommandQueue.clear();
                btConnectionState = CONNECTING;
                Log.i(TAG, "Connecting to device");
                scanLeDevice(false);// will stop after first device detection
                showConnectingNotification();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                        } else {
                            mGatt = device.connectGatt(mContext, true, gattCallback);
                        }
                    }
                }, 2000);
                break;
            case CLASSIC:
                mBluetoothChat.connectBluetooth(device);
                break;
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

        if(btConnectionState == CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting");
            return;
        }

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null");
            return;
        }

        scanLeDevice(true);

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
                if(device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                    deviceInterface = new Device212B(mContext, dataListener, Bluetooth215BComm.this);
                    connectToDevice(device);
                } else if(device.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
                    deviceInterface = new Device215B(mContext, dataListener);
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
                    }, 2000);

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
                queueCommand(new WriteCommand(null, WriteCommand.WRITE_TYPE.NOTIFICATION, deviceInterface.getServiceUuid(),
                        deviceInterface.getWriteChar(), deviceInterface.getReadChar()));
                hasDiscoveredServices = true;

                dataListener.getBluetoothState(btConnectionState);

            } else {
                Log.i(TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, final int status) {

            final byte[] bytes = characteristic.getValue();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    deviceInterface.onCharacteristicRead(bytes, status);
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] bytes = characteristic.getValue();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    deviceInterface.onCharacteristicChanged(bytes);
                }
            });
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

    }

    // functions

    @Override
    public void getVin() {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getVin()); // 212 parser returns json
    }

    @Override
    public void getRtc() {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getRtc());
    }

    @Override
    public void setRtc(long rtcTime) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setRtc(rtcTime));
    }

    @Override
    public void getPids(String pids) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getPids(pids));
    }

    @Override
    public void getSupportedPids() {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getSupportedPids());
    }

    // sets pids to check and sets data interval
    @Override
    public void setPidsToSend(String pids) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setPidsToSend(pids));
    }

    @Override
    public void getDtcs() {
        if (btConnectionState != CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getDtcs());
    }





    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!mObdManager.isParse() && dataLists.size() > 0) {
                mObdManager.receiveDataAndParse(dataLists.get(0));
                dataLists.remove(0);
            }
            mHandler.postDelayed(this, 500);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "BluetoothClassicComm message handler");
            super.handleMessage(msg);
            switch (msg.what) {
                case CANCEL_DISCOVERY:
                {
                    Log.i(TAG,"CANCEL_DISCOVERY - BluetoothClassicComm");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    break;
                }
                case BLUETOOTH_CONNECT_SUCCESS:
                {
                    Log.i(TAG,"Bluetooth connect success - BluetoothClassicComm");
                    btConnectionState = CONNECTED;
                    Log.i(TAG, "Saving Mac Address - BluetoothClassicComm");
                    OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
                    Log.i(TAG, "setting dataListener - getting bluetooth state - BluetoothClassicComm");
                    dataListener.getBluetoothState(btConnectionState);

                    break;
                }
                case BLUETOOTH_CONNECT_FAIL:
                {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG, "Bluetooth connection failed - BluetoothClassicComm");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    OBDInfoSP.saveMacAddress(mContext, "");
                    Log.i(TAG,"Retry connection");
                    Log.i(TAG, "Sending out bluetooth state on dataListener");
                    dataListener.getBluetoothState(btConnectionState);
                    break;
                }
                case BLUETOOTH_CONNECT_EXCEPTION:
                {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG,"Bluetooth connection exception - calling get bluetooth state on dListener");
                    dataListener.getBluetoothState(btConnectionState);
                    break;
                }
                case BLUETOOTH_READ_DATA:
                {
                    if (!Utils.isEmpty(Utils.bytesToHexString((byte[]) msg.obj))) {
                        Log.v(TAG, "Bluetooth read data... - BluetoothClassicComm");
                        dataLists.add(Utils.bytesToHexString((byte[]) msg.obj));
                    }
                    break;
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BReceiver onReceive - BluetoothClassicComm");

            String action = intent.getAction();
            LogUtil.i(action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.v(TAG,"A device found - BluetoothClassicComm");
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.v(TAG,device.getName() + " " + device.getAddress());

                if (device.getName()!=null&&device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                    Log.i(TAG,"OBD device found... Connect to IDD-212 - BluetoothClassicComm");
                    mBluetoothChat.connectBluetooth(device);
                    Log.i(TAG,"Connecting to device - BluetoothClassicComm");
                    Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Log.i(TAG,"Phone is connected to a remote device - BluetoothClassicComm");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!=null && device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                    Log.i(TAG, "Connected to device: " + device.getName());
                    btConnectionState = CONNECTED;
                    LogUtil.i("Bluetooth state:CONNECTED");
                    try {
                        new MixpanelHelper(application).trackConnectionStatus(MixpanelHelper.CONNECTED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    dataListener.getBluetoothState(btConnectionState);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                Log.i(TAG,"Pairing state changed - BluetoothClassicComm");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()==BluetoothDevice.BOND_BONDED &&
                        (device.getName().contains(ObdManager.BT_DEVICE_NAME_212))) {
                    Log.i(TAG,"Connected to a PAIRED device - BluetoothClassicComm");
                    btConnectionState = CONNECTED;
                    dataListener.getBluetoothState(btConnectionState);
                }

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Log.i(TAG, "Disconnection from a remote device - BluetoothClassicComm");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getName()!= null && device.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    try {
                        new MixpanelHelper(application).trackConnectionStatus(MixpanelHelper.DISCONNECTED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dataListener.getBluetoothState(btConnectionState);
                }

                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(BluetoothAutoConnectService.notifID);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG,"Bluetooth state:ACTION_DISCOVERY_STARTED - BluetoothClassicComm");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG,"Bluetooth state:ACTION_DISCOVERY_FINISHED - BluetoothClassicComm");
                if (btConnectionState != CONNECTED) {
                    btConnectionState = DISCONNECTED;
                    Log.i(TAG,"Not connected - setting get bluetooth state on dListeners");
                    dataListener.getBluetoothState(btConnectionState);
                }
            }else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                //if(mBluetoothAdapter.isEnabled())
                //    connectBluetooth();
                //Log.i(TAG,"Bluetooth state:SCAN_MODE_CHNAGED- setting dListeners btState");
                //dataListener.getBluetoothState(btConnectionState);
            }
        }
    };

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, filter);
    }
}

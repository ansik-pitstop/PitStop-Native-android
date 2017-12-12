package com.castel.obd.bluetooth;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.bleDevice.AbstractDevice;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.utils.MixpanelHelper;

import java.io.UnsupportedEncodingException;
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
public class BluetoothLeComm implements BluetoothCommunicator {
    private static String TAG = BluetoothLeComm.class.getSimpleName();
    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private final LinkedList<WriteCommand> mCommandQueue = new LinkedList<>();
    //Command Operation executor - will only run one at a time
    ExecutorService mCommandExecutor;
    //Semaphore lock to coordinate command executions, to ensure only one is
    //currently started and waiting on a response.
    Semaphore mCommandLock = new Semaphore(1,true);

    private BluetoothGatt mGatt;
    private boolean hasDiscoveredServices = false;

    private UUID serviceUuid;
    private UUID writeChar;
    private UUID readChar;
    private AbstractDevice device;

    private int btConnectionState = DISCONNECTED;

    public BluetoothLeComm(Context context, AbstractDevice device) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        this.device = device;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public void setServiceUuid(UUID serviceUuid){
        Log.d(TAG,"setServiceUuid()");
        this.serviceUuid = serviceUuid;
    }

    public void setWriteChar(UUID writeChar){
        Log.d(TAG,"setWriteChar()");
        this.writeChar = writeChar;
    }

    public void setReadChar(UUID readChar){
        Log.d(TAG,"setReadChar()");
        this.readChar = readChar;
    }

    @Override
    public int getState() {
        return btConnectionState;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @Override
    public void close() {
        Log.d(TAG,"close()");
        btConnectionState = DISCONNECTED;
        hasDiscoveredServices = false;
        mCommandExecutor.shutdownNow();
        mCommandLock.release();
        mCommandQueue.clear();
        if (mGatt != null) {
            mGatt.close();
        }
    }


    @Override
    @SuppressLint("NewApi")
    public void connectToDevice(final BluetoothDevice device) {
        Log.d(TAG,"Connect to device()");
        mCommandQueue.clear();
        mCommandExecutor = Executors.newSingleThreadExecutor();
        new Handler().postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mGatt = device.connectGatt(mContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mGatt = device.connectGatt(mContext, true, gattCallback);
            }
        }, 2000);
    }

    @Override
    public void writeData(byte[] bytes) {
        if(!hasDiscoveredServices || serviceUuid == null || writeChar == null || readChar == null) {
            return;
        }

        if (btConnectionState == CONNECTED && mGatt != null) {
            queueCommand(new WriteCommand(bytes, WriteCommand.WRITE_TYPE.DATA
                    , serviceUuid, writeChar, readChar));
        }
    }



    private void queueCommand(WriteCommand command) {
        synchronized (mCommandQueue) {
            Log.i(TAG,"Queue command");
            mCommandQueue.add(command);
            //Schedule a new runnable to process that command (one command at a time executed only)
            ExecuteCommandRunnable runnable = new ExecuteCommandRunnable(command, mGatt);
            if (mCommandExecutor != null)
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

    public void bluetoothStateChanged(int state) {
        if(state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = DISCONNECTED;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange Status: " + status + " new State " + newState);
            switch (newState) {

                case BluetoothProfile.STATE_CONNECTING:
                    Log.i(TAG, "gattCallback STATE_CONNECTING");
                    btConnectionState = CONNECTING;
                    device.setManagerState(btConnectionState);
                    break;


                case BluetoothProfile.STATE_CONNECTED:
                    //Do not notify state as connected, it is done onServicesDiscovered
                    Log.i(TAG, "ACTION_GATT_CONNECTED");
                    mixpanelHelper.trackConnectionStatus(MixpanelHelper.CONNECTED);
                    gatt.discoverServices();
                    break;


                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.i(TAG, "gattCallback STATE_DISCONNECTING");
                    break;


                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "ACTION_GATT_DISCONNECTED");
                    btConnectionState = DISCONNECTED;
                    mixpanelHelper.trackConnectionStatus(MixpanelHelper.DISCONNECTED);
                    device.setManagerState(btConnectionState);
                    NotificationManager mNotificationManager =
                            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(BluetoothAutoConnectService.notifID);
                    break;


                default:
                    Log.i(TAG, "gattCallback STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (serviceUuid == null || writeChar == null || readChar == null) return;

            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                mCommandLock.release();
                queueCommand(new WriteCommand(null, WriteCommand.WRITE_TYPE.NOTIFICATION, serviceUuid,
                        writeChar, readChar));
                hasDiscoveredServices = true;
                // Setting bluetooth state as connected because, you can't communicate with
                // device until services have been discovered
                btConnectionState = CONNECTED;
                device.setManagerState(btConnectionState);

            } else {
                Log.i(TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {

            if (serviceUuid == null || writeChar == null || readChar == null) return;

            if (readChar.equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();
                Log.d(TAG, data.toString());
                String readData = "";

                try {
                    readData = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.v("onCharacteristicRead", "Data Read: " + readData.replace("\r", "\\r").replace("\n", "\\n"));

                if(data != null && data.length > 0 && status == BluetoothGatt.GATT_SUCCESS) {
                    device.parseData(data);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (serviceUuid == null || writeChar == null || readChar == null) return;

            if (readChar.equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();

                String readData = "";

                try {
                    readData = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.v("onCharacteristicChanged", "Data Read: " + readData.replace("\r", "\\r").replace("\n", "\\n"));

                if(data != null && data.length > 0) {
                    device.parseData(data);
                }
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

}
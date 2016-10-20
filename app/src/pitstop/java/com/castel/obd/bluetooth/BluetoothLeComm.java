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
import android.util.Log;

import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.Utils;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.ui.AddCarActivity;
import com.pitstop.ui.MainActivity;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

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
    private BluetoothGatt mGatt;
    private boolean mIsScanning = false;
    private boolean hasDiscoveredServices = false;

    private BluetoothDeviceManager deviceManager;

    private static String TAG = BluetoothLeComm.class.getSimpleName();

    private boolean needToScan = true; // need to scan after restarting bluetooth adapter even if mGatt != null

    private UUID serviceUuid;
    private UUID writeChar;
    private UUID readChar;

    private int btConnectionState = DISCONNECTED;

    public BluetoothLeComm(Context context, BluetoothDeviceManager deviceManager, UUID serviceUuid, UUID writeChar, UUID readChar) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        this.serviceUuid = serviceUuid;
        this.writeChar = writeChar;
        this.readChar = readChar;

        this.deviceManager = deviceManager;
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
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }


    @Override
    @SuppressLint("NewApi")
    public void connectToDevice(final BluetoothDevice device) {
        mCommandQueue.clear();
        new Handler().postDelayed(new Runnable() {
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
    }

    @Override
    public void writeData(byte[] bytes) {
        if(!hasDiscoveredServices) {
            return;
        }

        if (btConnectionState == CONNECTED && mGatt != null) {
            queueCommand(new WriteCommand(bytes, WriteCommand.WRITE_TYPE.DATA, serviceUuid, writeChar, readChar));
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
                    deviceManager.connectionStateChange(btConnectionState);
                    btConnectionState = CONNECTED;
                    gatt.discoverServices();

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
                    deviceManager.connectionStateChange(btConnectionState);
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
                queueCommand(new WriteCommand(null, WriteCommand.WRITE_TYPE.NOTIFICATION, serviceUuid,
                        writeChar, readChar));
                hasDiscoveredServices = true;

                // Setting bluetooth state as connected because, you can't communicate with
                // device until services have been discovered
                //btConnectionState = CONNECTED;
                deviceManager.connectionStateChange(btConnectionState);

            } else {
                Log.i(TAG, "Error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            if (readChar.equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();

                if(data != null && data.length > 0 && status == BluetoothGatt.GATT_SUCCESS) {
                    deviceManager.readData(data);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (readChar.equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();

                if(data != null && data.length > 0) {
                    deviceManager.readData(data);
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
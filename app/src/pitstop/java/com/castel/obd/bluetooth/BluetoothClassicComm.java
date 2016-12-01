package com.castel.obd.bluetooth;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.Utils;
import com.pitstop.bluetooth.BluetoothRecognizer;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
@SuppressLint("all")
public class BluetoothClassicComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {

    private static String TAG = "BtClassicComm";

    private int btConnectionState = DISCONNECTED;

    private Context mContext;
    private GlobalApplication application;

    private ObdManager mObdManager;

    private BluetoothChat mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;
    private final BluetoothRecognizer mBluetoothRecognizer;

    private boolean isMacAddress = false;

    private String connectedDeviceName;

    private List<String> dataLists = new ArrayList<>();

    private ObdManager.IBluetoothDataListener dataListener;

    public BluetoothClassicComm(Context context) {
        Log.i(TAG, "classicComm Constructor");
        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mObdManager = new ObdManager(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothChat = new BluetoothChat(mHandler);
        registerBluetoothReceiver();

        //int initSuccess = mObdManager.initializeObd();
        //Log.d(TAG, "init result: " + initSuccess);

        mHandler.postDelayed(runnable, 500);

        mBluetoothRecognizer = new BluetoothRecognizer(context);
    }

    @Override
    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
        mObdManager.setDataListener(this.dataListener);
        mObdManager.setPassiveCommandListener(this);
    }

    @Override
    public boolean hasDiscoveredServices() {
        return true;
    }

    @Override
    public void startScan() {
        connectBluetooth();
    }

    @Override
    public void stopScan() {

    }

    @Override
    public int getState() {
        return btConnectionState;
    }

    @Override
    public void obdSetCtrl(int type) {
        if (btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetCtrl(type);
        Log.i(TAG, "Set ctrl result: " + result);
        writeToObd(result);
    }

    @Override
    public void obdSetMonitor(int type, String valueList) {
        if (btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetMonitor(type, valueList);
        writeToObd(result);
    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {
        if (btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetParameter(tlvTagList, valueList);
        writeToObd(result);
    }

    @Override
    public void obdGetParameter(String tlvTag) {
        if (btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdGetParameter(tlvTag);
        writeToObd(result);
    }

    @Override
    public void sendCommandPassive(String instruction) {
        byte[] bytes = mObdManager.getBytesToSendPassive(instruction);

        if (bytes == null) {
            return;
        }

        if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
            mBluetoothChat.connectedThread.write(bytes);
        }
    }

    @Override
    public void close() {
        Log.i(TAG, "Closing connection - BluetoothClassicComm");
        connectedDeviceName = null;
        btConnectionState = DISCONNECTED;
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
        mBluetoothChat.closeConnect();
        mHandler.removeCallbacks(runnable);
    }

    @Override
    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    private void writeToObd(String payload) {
        byte[] bytes = mObdManager.getBytesToSend(payload);
        if (bytes == null) {
            return;
        }

        if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
            mBluetoothChat.connectedThread.write(bytes);
        }
    }

    private void connectBluetooth() {
        Log.d(TAG, "connectBluetooth()");

        if (btConnectionState == CONNECTED) {
            Log.i(TAG, "Bluetooth is connected - BluetoothClassicComm");
            return;
        }

        if (mBluetoothAdapter.isDiscovering() || btConnectionState == CONNECTING || mBluetoothChat.isConnecting()) {
            Log.i(TAG, "Already discovering - BluetoothClassicComm");
            Log.d(TAG, "mBluetoothAdapter is discovering: " + mBluetoothAdapter.isDiscovering());
            Log.d(TAG, "btState is connecting: " + (btConnectionState == CONNECTING));
            Log.d(TAG, "mBluetoothChat is connecting: " + (mBluetoothChat.isConnecting()));
            return;
        }

        Log.i(TAG, "Connecting to bluetooth - BluetoothClassicComm");
        btConnectionState = CONNECTING;
        mBluetoothChat.closeConnect();

        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled");
            mBluetoothAdapter.enable();
        }

        Log.i(TAG, "Getting saved macAddress - BluetoothClassicComm");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        if (!"".equals(macAddress)) {
            isMacAddress = true;
            Log.i(TAG, "Using macAddress " + macAddress + " to connect to device - BluetoothClassicComm");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            mBluetoothChat.connectBluetooth(device);
        } else {
            Log.i(TAG, "Starting discovery - BluetoothClassicComm");
            mBluetoothAdapter.startDiscovery();
        }
        mHandler.sendEmptyMessageDelayed(CANCEL_DISCOVERY, 14464);
    }

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

    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            connectedDeviceName = null;
            btConnectionState = DISCONNECTED;
        } else if (state == CONNECTED) {
            btConnectionState = state;
        }
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
                case CANCEL_DISCOVERY: {
                    Log.i(TAG, "CANCEL_DISCOVERY - BluetoothClassicComm");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    break;
                }
                case BLUETOOTH_CONNECT_SUCCESS: {
                    Log.i(TAG, "Bluetooth connect success - BluetoothClassicComm");
                    btConnectionState = CONNECTED;
                    Log.i(TAG, "Saving Mac Address - BluetoothClassicComm");
                    OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
                    Log.i(TAG, "setting dataListener - getting bluetooth state - BluetoothClassicComm");
                    dataListener.getBluetoothState(btConnectionState);

                    break;
                }
                case BLUETOOTH_CONNECT_FAIL: {
                    connectedDeviceName = null;
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG, "Bluetooth connection failed - BluetoothClassicComm");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    OBDInfoSP.saveMacAddress(mContext, "");
                    Log.i(TAG, "Retry connection");
                    Log.i(TAG, "Sending out bluetooth state on dataListener");
                    dataListener.getBluetoothState(btConnectionState);
                    break;
                }
                case BLUETOOTH_CONNECT_EXCEPTION: {
                    connectedDeviceName = null;
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG, "Bluetooth connection exception - calling get bluetooth state on dListener");
                    dataListener.getBluetoothState(btConnectionState);
                    break;
                }
                case BLUETOOTH_READ_DATA: {
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

            if (BluetoothDevice.ACTION_FOUND.equals(action)) { // Discovered the device
                Log.v(TAG, "DEVICE_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.v(TAG, device.getName() + " " + device.getAddress());
                String deviceName = device.getName();
                switch (mBluetoothRecognizer.onDeviceFound(deviceName)) { // Recognize
                    case CONNECT:
                        Log.d(TAG, "Recognized: CONNECTION granted");
                        // This method should always be called before attempting to connect to a remote device with BluetoothSocket.connect()
                        mBluetoothAdapter.cancelDiscovery();
                        int bondState = device.getBondState();
                        switch (bondState) {
                            case BluetoothDevice.BOND_NONE: // If not bonded, create bond (pair)
                                try {
                                    if (device.createBond()) {
                                        Log.d(TAG, "Bond creation will be done");
                                    } else {
                                        Log.d(TAG, "Error doing bond creation");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case BluetoothDevice.BOND_BONDED: // If bonded, connect
                                connectedDeviceName = deviceName;
                                mBluetoothChat.connectBluetooth(device);
                                Log.i(TAG, "Connecting to a paired device - BluetoothClassicComm");
                                Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
                                break;
                            case BluetoothDevice.BOND_BONDING: // If it bonding, wait for BOND_STATE_CHANGED
                                // do nothing
                        }
                        break;
                    default:
                        // Ignore otherwise
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) { // Low level connection
                Log.d(TAG, "ACL_CONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                int bondState = device.getBondState();
                if (deviceName != null && deviceName.contains(ObdManager.BT_DEVICE_NAME)) {
                    switch (bondState){
                        case BluetoothDevice.BOND_BONDED:
                            Log.i(TAG, "Connected to a PAIRED device: " + deviceName);
                            connectedDeviceName = deviceName;
                            btConnectionState = CONNECTED;
                            try {
                                new MixpanelHelper(application).trackConnectionStatus(MixpanelHelper.CONNECTED);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dataListener.getBluetoothState(btConnectionState);
                            Toast.makeText(mContext, "Connected to a paired device", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothDevice.BOND_NONE:
                            try {
                                if (device.createBond()) { // true if bond creation will be done
                                    Log.d(TAG, "Bond creation will be done");
                                } else { // false if immediate error
                                    Log.d(TAG, "Error doing bond creation");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            // do nothing
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.d(TAG, "BOND_STATE_CHANGED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (!deviceName.contains(ObdManager.BT_DEVICE_NAME)) return;

                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDING);
                Log.d(TAG, "PREVIOUS: " + previousBondState);
                Log.d(TAG, "CURRENT: " + bondState);

                switch (bondState){
                    case BluetoothDevice.BOND_BONDED:
                        // If bonded, start connecting
                        connectedDeviceName = deviceName;
                        mBluetoothChat.connectBluetooth(device);
                        Log.i(TAG, "Connecting to device - BluetoothClassicComm");
                        Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        if (previousBondState == BluetoothDevice.BOND_BONDING){
                            try {
                                if (device.createBond()) {
                                    Log.d(TAG, "Bond creation will be done");
                                } else {
                                    Log.d(TAG, "Error doing bond creation");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        // do nothing
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACL_DISCONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                    btConnectionState = DISCONNECTED;
                    connectedDeviceName = null;
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
                Log.i(TAG, "Bluetooth state:ACTION_DISCOVERY_STARTED - BluetoothClassicComm");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Bluetooth state:ACTION_DISCOVERY_FINISHED - BluetoothClassicComm");
                if (btConnectionState != CONNECTED) {
                    btConnectionState = DISCONNECTED;
                    Log.i(TAG, "Not connected - setting get bluetooth state on dListeners");
                    connectedDeviceName = null;
                    dataListener.getBluetoothState(btConnectionState);
                }
            }
        }
    };
}

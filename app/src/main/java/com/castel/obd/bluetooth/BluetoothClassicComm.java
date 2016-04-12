package com.castel.obd.bluetooth;

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
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.Utils;
import com.pitstop.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public class BluetoothClassicComm implements IBluetoothCommunicator, ObdManager.IPassiveCommandListener {
    private int btConnectionState = DISCONNECTED;

    private Context mContext;
    private ObdManager mObdManager;

    private BluetoothChat mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean isMacAddress = false;

    private List<String> dataLists = new ArrayList<String>();

    private ObdManager.IBluetoothDataListener dataListener;

    public BluetoothClassicComm(Context context) {
        Log.i(MainActivity.TAG, "classicComm Constructor");
        mContext = context;
        mObdManager = new ObdManager(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothChat = new BluetoothChat(mHandler);
        registerBluetoothReceiver();
        mObdManager.initializeObd();
        mHandler.postDelayed(runnable, 500);
    }

    @Override
    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
        mObdManager.setDataListener(this.dataListener);
        mObdManager.setPassiveCommandListener(this);
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
        if(btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetCtrl(type);
        writeToObd(result);
    }

    @Override
    public void obdSetMonitor(int type, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetMonitor(type, valueList);
        writeToObd(result);
    }

    @Override
    public void obdSetParameter(String tlvTagList, String valueList) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdSetParameter(tlvTagList, valueList);
        writeToObd(result);
    }

    @Override
    public void obdGetParameter(String tlvTag) {
        if(btConnectionState != CONNECTED) {
            return;
        }

        String result = mObdManager.obdGetParameter(tlvTag);
        writeToObd(result);
    }

    @Override
    public void sendCommandPassive(String instruction) {
        byte[] bytes = mObdManager.getBytesToSendPassive(instruction);

        if(bytes == null) {
            return;
        }

        if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
            mBluetoothChat.connectedThread.write(bytes);
        }
    }

    @Override
    public void close() {
        Log.i(MainActivity.TAG,"Closing connection - BluetoothManage");
        btConnectionState = DISCONNECTED;
        mContext.unregisterReceiver(mReceiver);
        mBluetoothChat.closeConnect();
        mHandler.removeCallbacks(runnable);
    }

    private void writeToObd(String payload) {

        byte[] bytes = mObdManager.getBytesToSend(payload);
        if(bytes == null) {
            return;
        }

        if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
            mBluetoothChat.connectedThread.write(bytes);
        }
    }

    private void connectBluetooth() {

        if (btConnectionState == CONNECTED) {
            Log.i(MainActivity.TAG,"Bluetooth is connected - BluetoothClassicComm");
            return;
        }

        Log.i(MainActivity.TAG,"Connecting to bluetooth - BluetoothClassicComm");
        btConnectionState = CONNECTING;
        mBluetoothChat.closeConnect();

        if (!mBluetoothAdapter.isEnabled()) {
            LogUtil.i("BluetoothAdapter.enable()");
            Log.i(MainActivity.TAG,"Bluetooth not enabled");
            mBluetoothAdapter.enable();
        }

        Log.i(MainActivity.TAG,"Getting saved macAddress - BluetoothManage");
        String macAddress = OBDInfoSP.getMacAddress(mContext);

        if (!"".equals(macAddress)) {
            isMacAddress = true;
            Log.i(MainActivity.TAG,"Using macAddress "+macAddress+" to connect to device - BluetoothManage");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            mBluetoothChat.connectBluetooth(device);
        } else {
            LogUtil.i("startDiscovery()");
            Log.i(MainActivity.TAG,"Starting discovery - BluetoothManage");
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
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
            Log.i(MainActivity.TAG, "BluetoothManage message handler");
            super.handleMessage(msg);
            switch (msg.what) {
                case CANCEL_DISCOVERY:
                {
                    Log.i(MainActivity.TAG,"CANCEL_DISCOVERY - BluetoothManage");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    break;
                }
                case BLUETOOTH_CONNECT_SUCCESS:
                {
                    Log.i(MainActivity.TAG,"Bluetooth connect success - BluetoothManage");
                    btConnectionState = CONNECTED;
                    Log.i(MainActivity.TAG, "Saving Mac Address - BluetoothManage");
                    OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
                    Log.i(MainActivity.TAG, "setting dataListener - getting bluetooth state - BluetoothManage");
                    dataListener.getBluetoothState(btConnectionState);

                    break;
                }
                case BLUETOOTH_CONNECT_FAIL:
                {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(MainActivity.TAG, "Bluetooth connection failed - BluetoothManage");
                    Log.i(MainActivity.TAG, "Bluetooth connection failed - BluetoothManage: Bool - Value: "+isMacAddress);
                    if (isMacAddress) {
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        Log.i(MainActivity.TAG,"Retry connection");
                        mBluetoothAdapter.startDiscovery();
                    } else {
                        Log.i(MainActivity.TAG, "Sending out bluetooth state on dataListener");
                        dataListener.getBluetoothState(btConnectionState);
                    }
                    break;
                }
                case BLUETOOTH_CONNECT_EXCEPTION:
                {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(MainActivity.TAG,"Bluetooth connection exception - calling get bluetooth state on dListener");
                    dataListener.getBluetoothState(btConnectionState);
                    break;
                }
                case BLUETOOTH_READ_DATA:
                {
                    if (!Utils.isEmpty(Utils.bytesToHexString((byte[]) msg.obj))) {
                        Log.i(MainActivity.TAG, "Bluetooth read data... - BluetoothManage");
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
            Log.i(MainActivity.TAG, "BReceiver onReceive - BluetoothManage");

            String action = intent.getAction();
            LogUtil.i(action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i(MainActivity.TAG,"A device found - BluetoothManage");
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(MainActivity.TAG,device.getName() + device.getAddress());

                if (device.getName()!=null&&device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                    Log.i(MainActivity.TAG,"OBD device found... Connect to IDD-212 - BluetoothManage");
                    mBluetoothChat.connectBluetooth(device);
                    Log.i(MainActivity.TAG,"Connecting to device - BluetoothManage");
                    Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.i(MainActivity.TAG,"Phone is connected to a remote device - BluetoothManage");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getName()!=null && device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                    Log.i(MainActivity.TAG, "Connected to device: " + device.getName());
                    btConnectionState = CONNECTED;
                    LogUtil.i("Bluetooth state:CONNECTED");
                    dataListener.getBluetoothState(btConnectionState);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                Log.i(MainActivity.TAG,"Pairing state changed - BluetoothManage");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()==BluetoothDevice.BOND_BONDED &&
                        (device.getName().contains(ObdManager.BT_DEVICE_NAME))) {
                    Log.i(MainActivity.TAG,"Connected to a PAIRED device - BluetoothManage");
                    LogUtil.i("CONNECTED");
                    btConnectionState = CONNECTED;
                    LogUtil.i("Bluetooth state:CONNECTED");
                    dataListener.getBluetoothState(btConnectionState);
                }

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

                Log.i(MainActivity.TAG, "Disconnection from a remote device - BluetoothManage");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getName()!= null && device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    dataListener.getBluetoothState(btConnectionState);
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(MainActivity.TAG,"Bluetooth state:ACTION_DISCOVERY_STARTED - BluetoothManage");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(MainActivity.TAG,"Bluetooth state:ACTION_DISCOVERY_FINISHED - BluetoothManage");
                if (btConnectionState != CONNECTED) {
                    btConnectionState = DISCONNECTED;
                    Log.i(MainActivity.TAG,"Not connected - setting get bluetooth state on dListeners");
                    dataListener.getBluetoothState(btConnectionState);
                }
            }else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                if(mBluetoothAdapter.isEnabled())
                    connectBluetooth();
                Log.i(MainActivity.TAG,"Bluetooth state:SCAN_MODE_CHNAGED- setting dListeners btState");
                dataListener.getBluetoothState(btConnectionState);
            }
        }
    };

    private void registerBluetoothReceiver() {
        Log.i(MainActivity.TAG,"Registering bluetooth intent receivers");
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

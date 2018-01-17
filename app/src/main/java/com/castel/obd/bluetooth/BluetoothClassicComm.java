package com.castel.obd.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.castel.obd.bleDevice.AbstractDevice;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.LogUtil;
import com.pitstop.application.GlobalApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public class BluetoothClassicComm implements BluetoothCommunicator {

    private int btConnectionState = DISCONNECTED;

    private Context mContext;
    private GlobalApplication application;
    private ObdManager mObdManager;
    private AbstractDevice device;
    private BluetoothChat mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isMacAddress = false;

    private List<byte[]> dataLists = new ArrayList<>();

    private static String TAG = BluetoothClassicComm.class.getSimpleName();

    public BluetoothClassicComm(Context context,  AbstractDevice device) {
        Log.i(TAG, "classicComm Constructor");
        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mObdManager = new ObdManager(context);
        this.device = device;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothChat = new BluetoothChat(mHandler);
        mHandler.postDelayed(runnable, 500);
    }

    @Override
    public int getState() {
        return btConnectionState;
    }

    @Override
    public void close() {
        Log.i(TAG, "Closing connection - BluetoothClassicComm");
        btConnectionState = DISCONNECTED;
        device.onConnectionStateChange(DISCONNECTED);
        mBluetoothChat.closeConnect();
        mHandler.removeCallbacks(runnable);
    }

    @Override
    public void writeData(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
            mBluetoothChat.connectedThread.write(bytes);
        }
    }

    @Override
    public void connectToDevice(BluetoothDevice device) {
        mBluetoothChat.connectBluetooth(device);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!mObdManager.isParse() && dataLists.size() > 0) {
                device.parseData(dataLists.get(0));
                dataLists.remove(0);
            }
            mHandler.postDelayed(this, 500);
        }
    };

    private final Handler mHandler = new Handler() {
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
                    btConnectionState = CONNECTED;
                    Log.i(TAG, "Bluetooth connect success - BluetoothClassicComm");
                    Log.i(TAG, "Saving Mac Address - BluetoothClassicComm");
                    OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
                    Log.i(TAG, "setting dataListener - getting bluetooth state - BluetoothClassicComm");
                    device.onConnectionStateChange(btConnectionState);

                    break;
                }
                case BLUETOOTH_CONNECT_FAIL: {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG, "Bluetooth connection failed - BluetoothClassicComm");
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    OBDInfoSP.saveMacAddress(mContext, "");
                    Log.i(TAG, "Retry connection");
                    Log.i(TAG, "Sending out bluetooth state on dataListener");
                    device.onConnectionStateChange(btConnectionState);
                    break;
                }
                case BLUETOOTH_CONNECT_EXCEPTION: {
                    btConnectionState = DISCONNECTED;
                    LogUtil.i("Bluetooth state:DISCONNECTED");
                    Log.i(TAG, "Bluetooth connection exception - calling get bluetooth state on dListener");
                    device.onConnectionStateChange(btConnectionState);
                    break;
                }
                case BLUETOOTH_READ_DATA: {
                    if (msg.obj != null && ((byte[]) msg.obj).length > 0) {
                        Log.v(TAG, "Bluetooth read data... - BluetoothClassicComm");
                        dataLists.add((byte[]) msg.obj);
                    }
                    break;
                }
            }
        }
    };

    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = DISCONNECTED;
        } else if (state == CONNECTED) {
            btConnectionState = state;
        }
    }
}

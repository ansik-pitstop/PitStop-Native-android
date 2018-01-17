package com.pitstop.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.castel.obd.bleDevice.ELM327Device;
import com.castel.obd.bluetooth.BluetoothChatElm327;
import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.elm.commands.ObdCommand;

import static com.castel.obd.bluetooth.IBluetoothCommunicator.NO_DATA;

/**
 * Created by ishan on 2017-12-11.
 */

public class BluetoothCommunicatorELM327 implements BluetoothCommunicator {

    public static final String TAG = BluetoothCommunicatorELM327.class.getSimpleName();
    private ELM327Device ELM327;
    private int btConnectionState = DISCONNECTED;
    private BluetoothChatElm327 mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;



    public BluetoothCommunicatorELM327(Context context, ELM327Device ELM327){
        this.ELM327 = ELM327;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothChat = new BluetoothChatElm327(mHandler);
    }

    @Override
    public boolean writeData(byte[] bytes) {
        //Remove this
        return false;
    }

    public void writeData(ObdCommand obdCommand){
        if (btConnectionState != CONNECTED || !mBluetoothChat.isConnected()){
            Log.d(TAG, "not Connected");
            return;
        }
        if (obdCommand.getName()!=null)
            // some commands dont have a name or return results as name which is null when being
            Log.d(TAG, "sending command: " + obdCommand.getName());

        mBluetoothChat.connectedThread.SendCommand(obdCommand);
    }

    @Override
    public void connectToDevice(BluetoothDevice device) {
        mBluetoothChat.connectBluetooth(device);
    }

    @Override
    public int getState() {
        return btConnectionState;
    }

    @Override
    public void close() {
        Log.d(TAG,"close()");
        btConnectionState = DISCONNECTED;
        ELM327.onConnectionStateChange(btConnectionState);
        mBluetoothChat.closeConnect();

    }
    @Override
    public void bluetoothStateChanged(int state) {
        if(state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = DISCONNECTED;
        }
    }



    @SuppressLint("HandlerLeak")
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
                    Log.d(TAG, "BluetoothConnection success");
                    btConnectionState = CONNECTED;
                    ELM327.onConnectionStateChange(btConnectionState);
                    break;
                }
                case BLUETOOTH_CONNECT_FAIL: {
                    btConnectionState = DISCONNECTED;
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    ELM327.onConnectionStateChange(btConnectionState);
                    break;
                }
                case NO_DATA: {
                    Log.w(TAG, "noData");
                    if (msg.obj != null && msg.obj instanceof ObdCommand) {
                        if (((ObdCommand) msg.obj).getName()!=null) {
                            Log.v(TAG, ((ObdCommand) msg.obj).getName() + " command got no data");
                            ELM327.noData((ObdCommand) msg.obj);
                        }
                    }
                    break;
                }
                case BLUETOOTH_CONNECT_EXCEPTION: {
                    btConnectionState = DISCONNECTED;
                    ELM327.onConnectionStateChange(btConnectionState);
                    break;
                }
                case BLUETOOTH_READ_DATA: {
                    if (msg.obj != null && msg.obj instanceof ObdCommand) {
                        Log.v(TAG, "Bluetooth read data- ELM 327 ");
                        ELM327.parseData((ObdCommand)msg.obj);
                    }
                    break;
                }
            }
        }
    };



}

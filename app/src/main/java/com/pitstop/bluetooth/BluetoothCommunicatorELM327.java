package com.pitstop.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.castel.obd.bleDevice.ELM327Device;
import com.castel.obd.bluetooth.BluetoothChat;
import com.castel.obd.bluetooth.BluetoothChatElm327;
import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.util.LogUtil;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.castel.obd.bluetooth.IBluetoothCommunicator.NO_DATA;

/**
 * Created by ishan on 2017-12-11.
 */

public class BluetoothCommunicatorELM327 implements BluetoothCommunicator {

    public static final String TAG = BluetoothCommunicatorELM327.class.getSimpleName();
    private ELM327Device ELM327;
    private BluetoothDevice device;

    private int btConnectionState = DISCONNECTED;
    private BluetoothSocket socket;
    private BluetoothChatElm327 mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothSocket getSocket(){
        return this.socket;
    }

    public BluetoothCommunicatorELM327(Context context, ELM327Device ELM327){
        this.ELM327 = ELM327;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothChat = new BluetoothChatElm327(mHandler);
    }

    @Override
    public void writeData(byte[] bytes) {

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
        ELM327.setManagerState(btConnectionState);
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
                    ELM327.setManagerState(btConnectionState);
                    // set up the device
                    writeData(new EchoOffCommand());
                    writeData(new LineFeedOffCommand());
                    writeData(new TimeoutCommand(125));
                    writeData(new SelectProtocolCommand(ObdProtocols.AUTO));
                    break;
                }
                case BLUETOOTH_CONNECT_FAIL: {
                    btConnectionState = DISCONNECTED;
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    ELM327.setManagerState(btConnectionState);
                    break;
                }
                case NO_DATA: {
                    Log.w(TAG, "noData");
                }
                case BLUETOOTH_CONNECT_EXCEPTION: {
                    btConnectionState = DISCONNECTED;
                    ELM327.setManagerState(btConnectionState);
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

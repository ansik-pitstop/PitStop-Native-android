package com.castel.obd.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.util.Utils;
import com.facebook.stetho.inspector.protocol.module.Network;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.exceptions.NoDataException;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by ishan on 2017-12-12.
 */

public class BluetoothChatElm327 {

    private final static String TAG = BluetoothChatElm327.class.getSimpleName();
    public ConnectThread connectThread;
    public ConnectedThread connectedThread;
    public Handler mHandler;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothChatElm327(Handler handler) {
        this.mHandler = handler;
    }

    public void closeConnect() {
        Log.w(TAG, "Closing connection threads");
        if (null != connectedThread) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (null != connectThread) {
            connectThread.cancel();
            connectThread = null;
        }
    }


    public boolean isConnecting() {
        return connectThread != null && connectThread.isAlive();
    }

    public boolean isConnected() {
        return connectedThread != null;
    }

    public synchronized void connectBluetooth(BluetoothDevice device) {
        Log.d(TAG, "ConnectBluetooth: " + device.getName());
        if (isConnecting()) {
            Log.d(TAG, "already Connecting");
            return;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        @SuppressLint("NewApi")
        public ConnectThread(BluetoothDevice device) {

            mmDevice = device;

        }

        @Override
        public void run() {
            mHandler.sendEmptyMessage(IBluetoothCommunicator.CANCEL_DISCOVERY);
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket.connect();
                mHandler.sendMessage(mHandler.obtainMessage(
                        IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS,
                        mmDevice.getAddress()));

                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.start();

            } catch (Exception e1) {
                e1.printStackTrace();
                Log.d(TAG, "tryingFallbacksocket");
                if (mmSocket.isConnected()) {
                    Log.d(TAG, "already connected");
                }else {
                    Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                    Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                    try {
                        Log.i(TAG, "trying fallback connection");
                        Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                        Object[] params = new Object[]{Integer.valueOf(1)};
                        BluetoothSocket sockFallback = (BluetoothSocket) m.invoke(mmSocket.getRemoteDevice(), params);
                        sockFallback.connect();
                        mHandler.sendMessage(mHandler.obtainMessage(
                                IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS,
                                mmDevice.getAddress()));
                        connectedThread = new ConnectedThread(sockFallback);
                        connectedThread.start();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        mHandler.sendEmptyMessage(IBluetoothCommunicator.DISCONNECTED);
                        Log.i(TAG, "fallback connection didnt work, failed to connect");
                    }
                }
            }
        }

        public void cancel() {
            try {
                if (null != mmSocket) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // handle read and write from device
    public class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private ObdCommand obdCommand = new EchoOffCommand();
        private ReadResponseThread responseThread;

        /* The fallback thread is used because of a known android bug where the first socket fails
        ** when the exception is caught you connect to the device again.
        **SEE:  https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
         */


        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "Creating connected thread");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "socketAssigning threw exception");
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            responseThread = new ReadResponseThread();
            responseThread.setCommand(obdCommand);
        }

        public synchronized void SendCommand(ObdCommand obdCommand) {
            Log.d(TAG, "sendCommand: " + obdCommand.getName());
            this.obdCommand = obdCommand;
            responseThread.setCommand(obdCommand);
            mHandler.post(this);
        }

        @Override
        public void run() {
            Log.w(TAG, "Running connected thread");
            if (mmSocket == null) {
                Log.d(TAG, "Bluetooth Is null");
                return;
            }
            if (this.obdCommand == null) {
                Log.d(TAG, "obdCommand is null");
            } else {
                try {
                    obdCommand.run(mmInStream, mmOutStream);
                    mHandler.postDelayed(responseThread, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e instanceof NoDataException) {
                        Log.d(TAG, "no data Exception");
                        mHandler.sendEmptyMessage(IBluetoothCommunicator.NO_DATA);
                    }
                    if (!isConnected()) {
                        mHandler.sendEmptyMessage(IBluetoothCommunicator.DISCONNECTED);
                    }

                }
            }
        }


        public class ReadResponseThread extends Thread {
            private ObdCommand obdCommand;

            public void setCommand(ObdCommand cmd) {
                Log.d(TAG, "response thread set command " + cmd.getName());
                this.obdCommand = cmd;
            }

            public ReadResponseThread() {
                Log.d(TAG, "response thread created");
                this.obdCommand = obdCommand;

            }

            @Override
            public void run() {
                Log.d(TAG, "ResponseThreadRun");
                Log.d(TAG, "obd command: " + this.obdCommand.getName() + " value: " + obdCommand.getFormattedResult());
                mHandler.sendMessage(mHandler.obtainMessage(IBluetoothCommunicator.BLUETOOTH_READ_DATA, obdCommand));

            }
        }


        public void cancel() {
            try {
                if (null != mmOutStream) {
                    mmOutStream.close();
                    mmOutStream = null;
                }
                if (null != mmInStream) {
                    mmInStream.close();
                    mmInStream = null;
                }
                if (mmSocket != null) {
                    mmSocket.close();
                    mmSocket = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

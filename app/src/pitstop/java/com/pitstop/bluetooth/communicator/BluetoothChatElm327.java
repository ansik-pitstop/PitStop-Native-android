package com.pitstop.bluetooth.communicator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.elm.commands.ObdCommand;
import com.pitstop.bluetooth.elm.exceptions.NoDataException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ishan on 2017-12-12.
 */

public class BluetoothChatElm327 {

    private final static String TAG = BluetoothChatElm327.class.getSimpleName();
    public ConnectThread connectThread;
    public ConnectedThread connectedThread;
    public Handler mHandler;
    private BlockingQueue<ObdCommand> commandQueue; //Blocks until next command is received in thread

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothChatElm327(Handler handler) {
        commandQueue = new LinkedBlockingQueue<>();
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
                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.start();
                mHandler.sendMessage(mHandler.obtainMessage(
                        IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS,
                        mmDevice.getAddress()));

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
                        connectedThread = new ConnectedThread(sockFallback);
                        connectedThread.start();
                        mHandler.sendMessage(mHandler.obtainMessage(
                                IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS,
                                mmDevice.getAddress()));
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
        //private ReadResponseThread responseThread;

        /* The fallback thread is used because of a known android bug where the first socket fails
        ** when the exception is caught you connect to the device again.
        **SEE:  https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
         */
        ConnectedThread(BluetoothSocket socket) {
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
        }

        public synchronized void SendCommand(ObdCommand obdCommand) {
            if (obdCommand.getName()!=null)
                Log.d(TAG, "sendCommand: " + obdCommand.getName());
            try{
                commandQueue.put(obdCommand);
            }catch(Exception e){
                e.printStackTrace();
            }
            Log.d(TAG,"After adding write command queue size(): "+commandQueue.size());
        }

        @Override
        public void run() {
            ObdCommand obdCommand;

            while (true){
                try{
                    obdCommand = commandQueue.take(); //This will block until next command arrives
                    Log.d(TAG,"received command from queue: "+obdCommand.getName());
                }catch(InterruptedException e){
                    e.printStackTrace();
                    return;
                }

                if (mmSocket == null) {
                    Log.d(TAG, "Bluetooth is null");
                    return;
                }

                try {
                    obdCommand.run(mmInStream, mmOutStream);
                    mHandler.sendMessage(mHandler.obtainMessage(
                            IBluetoothCommunicator.BLUETOOTH_READ_DATA, obdCommand));
                } catch (Exception e) {
                    Log.d(TAG,"exception for command: "+obdCommand.getName());
                    e.printStackTrace();
                    if (e instanceof NoDataException) {
                        // In the ELM protocol, if you ask for a PID which isnt supported or soemthing for whi
                        // which the car ECU doesnt have data for, the device sends back "NODATA",
                        // in this case a NoDataException is thrown.
                        Log.d(TAG, "no data Exception");
                        mHandler.sendMessage(mHandler.obtainMessage(
                                IBluetoothCommunicator.NO_DATA, obdCommand));
                    }
                    if (!isConnected()) {
                        mHandler.sendEmptyMessage(IBluetoothCommunicator.DISCONNECTED);
                        return;
                    }

                }
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

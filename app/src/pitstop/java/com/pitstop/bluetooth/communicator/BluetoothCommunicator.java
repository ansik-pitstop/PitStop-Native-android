package com.pitstop.bluetooth.communicator;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Ben Wu on 2016-09-01.
 */
public interface BluetoothCommunicator {

    int BLUETOOTH_CONNECT_SUCCESS = 0;
    int BLUETOOTH_CONNECT_FAIL = 1;
    int BLUETOOTH_CONNECT_EXCEPTION = 2;
    int BLUETOOTH_READ_DATA = 4;
    int CANCEL_DISCOVERY = 5;

    int CONNECTED = 0;
    int DISCONNECTED = 1;
    int CONNECTING = 2;

    boolean writeData(byte[] bytes);

    void connectToDevice(BluetoothDevice device);

    int getState();
    void close();
    void bluetoothStateChanged(int state);

}

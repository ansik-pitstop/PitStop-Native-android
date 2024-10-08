package com.pitstop.bluetooth.bleDevice;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public interface AbstractDevice {

    UUID getServiceUuid();
    UUID getReadChar();
    UUID getWriteChar();
    byte[] getBytes(String payload);
    void parseData(byte[] data);
    void onConnectionStateChange(int state);
    void requestData(); // for 215 to ask for IDR

    // parameters
    boolean getVin();
    boolean getRtc();
    boolean setRtc(long rtcTime);
    boolean getPids(String pids);
    boolean getSupportedPids();
    boolean setPidsToSend(String pids, int timeInterval);
    boolean requestSnapshot();

    // monitor
    boolean clearDtcs();
    boolean getDtcs(); // stored
    boolean getPendingDtcs(); // pending
    boolean getFreezeFrame(); // FF

    boolean connectToDevice(BluetoothDevice device);
    boolean closeConnection();
    boolean setCommunicatorState(int state);
    int getCommunicatorState();
}

package com.pitstop.bluetooth.communicator;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public interface IBluetoothCommunicator {
    int BLUETOOTH_CONNECT_SUCCESS = 0;
    int BLUETOOTH_CONNECT_FAIL = 1;
    int BLUETOOTH_CONNECT_EXCEPTION = 2;
    int BLUETOOTH_READ_DATA = 4;
    int CANCEL_DISCOVERY = 5;
    int NO_DATA = 6;

    int CONNECTED = 0;
    int DISCONNECTED = 1;
    int CONNECTING = 2;

    void startScan();
    void stopScan();
    int getState();
    void obdSetCtrl(int type);
    void obdSetMonitor(int type, String valueList);
    void obdSetParameter(String tlvTagList, String valueList);
    void obdGetParameter(String tlvTag);
    void writeRawInstruction(String instruction);
    void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener);
    boolean hasDiscoveredServices();
    void close();
    void initDevice();
    void bluetoothStateChanged(int state);

    // parameters
    void getVin();
    void getRtc();
    void setRtc(long rtcTime);
    void getPids(String pids);
    void getSupportedPids();
    void setPidsToSend(String pids);

    // monitor
    void getDtcs(); // pending and stored

    String getConnectedDeviceName();
}

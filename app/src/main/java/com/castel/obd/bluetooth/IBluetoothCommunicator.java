package com.castel.obd.bluetooth;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public interface IBluetoothCommunicator {
    int BLUETOOTH_CONNECT_SUCCESS = 0;
    int BLUETOOTH_CONNECT_FAIL = 1;
    int BLUETOOTH_CONNECT_EXCEPTION = 2;
    int BLUETOOTH_READ_DATA = 4;
    int CANCEL_DISCOVERY = 5;

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
    void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener);
    boolean hasDiscoveredServices();
    void close();
    void bluetoothStateChanged(int state);
    String getConnectedDeviceName();
}

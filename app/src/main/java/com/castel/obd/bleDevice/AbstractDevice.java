package com.castel.obd.bleDevice;

import com.castel.obd.bluetooth.BluetoothDeviceManager;

import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public interface AbstractDevice {

    UUID getServiceUuid();

    UUID getReadChar();

    UUID getWriteChar();

    BluetoothDeviceManager.CommType commType();

    byte[] getBytes(String payload);

    void parseData(byte[] data);

    // parameters
    String getVin();
    String getRtc();
    String setRtc(long rtcTime);
    String getPids(String pids);
    String getSupportedPids();
    String setPidsToSend(String pids);

    // monitor
    String getDtcs(); // pending and stored

}

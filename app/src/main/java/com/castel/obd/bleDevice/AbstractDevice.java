package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public interface AbstractDevice {

    UUID getServiceUuid();

    UUID getReadChar();

    UUID getWriteChar();

    byte[] getBytes(String payload);

    void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status);

    void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    // parameters
    String getVin();
    String getRtc();
    String setRtc(long rtcTime);
    String getSupportedPids();
    String setPidsToSend(String pids);

    // monitor
    String getDtcs(); // pending and stored

}

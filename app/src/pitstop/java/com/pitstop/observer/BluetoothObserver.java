package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothObserver{
    void onSearchingForDevice();
    void onDeviceReady(String vin, String scannerId, String scannerName);
    void onDeviceDisconnected();
}

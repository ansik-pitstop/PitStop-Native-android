package com.pitstop.observer;

import com.pitstop.bluetooth.BluetoothAutoConnectService;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothObserver{
    void onSearchingForDevice();
    void onDeviceReady(BluetoothAutoConnectService bluetoothAutoConnectService);
    void onDeviceDisconnected();
}

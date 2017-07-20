package com.pitstop.observer;

import com.pitstop.bluetooth.BluetoothAutoConnectService;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothObservable<T> extends Subject<T>{
    void notifySearchingForDevice();
    void notifyDeviceReady();
    void notifyDeviceDisconnected();
    BluetoothAutoConnectService getBluetoothAutoConnectService();
}

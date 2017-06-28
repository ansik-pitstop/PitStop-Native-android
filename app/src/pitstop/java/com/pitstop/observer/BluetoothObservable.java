package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothObservable<T> extends Subject<T>{
    void notifyDeviceConnected();
    void notifyDeviceDisconnected();
}

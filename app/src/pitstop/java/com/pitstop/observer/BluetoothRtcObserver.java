package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 8/23/2017.
 */

public interface BluetoothRtcObserver {
    void onGotDeviceTime(long time);
    void onErrorGettingDeviceTime();
}

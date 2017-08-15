package com.pitstop.bluetooth;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public interface ConnectionStatusObserver {

    //Bluetooth has been turned on
    void onBluetoothOn();

    //Bluetooth has been turned off
    void onBluetoothOff();

    //Internet connection status changed to available
    void  onConnectedToInternet();
}

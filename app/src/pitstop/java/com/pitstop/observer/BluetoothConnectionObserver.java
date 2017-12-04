package com.pitstop.observer;

import com.pitstop.models.ReadyDevice;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObserver extends Observer{

    //Searching for bluetooth device associated with user car
    void onSearchingForDevice();

    //Device is ready to be interacted with
    void onDeviceReady(ReadyDevice readyDevice);

    //Device that was previously ready now disconnected and can no longer be interacted with
    void onDeviceDisconnected();

    //Device is connected and now being verified
    void onDeviceVerifying();

    //Device has been verified and is now syncing rtc time
    void onDeviceSyncing();

    void onGotSuportedPIDs(String value);

    void onConnectingToDevice();

    void onFoundDevices();
}

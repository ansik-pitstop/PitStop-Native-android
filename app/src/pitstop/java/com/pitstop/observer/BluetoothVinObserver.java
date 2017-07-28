package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 7/27/2017.
 */

public interface BluetoothVinObserver extends Observer {

    //VIN retrieved from the device that is currently being used
    void onGotVin(String vin);
}

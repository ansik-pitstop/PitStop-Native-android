package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObservable extends Subject{

    //Invoked if device recognized as broken and requires id overwrite
    void notifyDeviceNeedsOverwrite();

    //Invoked if observable is in the process of searching for a device
    void notifySearchingForDevice();

    //Invoked if device has been successfully paired and is ready for use
    void notifyDeviceReady(String vin, String scannerId, String scannerName);

    //Invoked if the currently used scanner has disconnected
    void notifyDeviceDisconnected();


}

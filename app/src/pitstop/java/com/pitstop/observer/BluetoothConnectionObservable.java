package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;

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

    //Invoked when the device is connected and now being verified
    void notifyVerifyingDevice();

    //Invoked when the device is verified and now being synced
    void notifySyncingDevice();

    //Invoked if dtc data has been received from the device
    void notifyDtcData(DtcPackage dtcPackage);

    //Invoked when the VIN has been retrieved from the vehicle
    void notifyVIN(String vin, String scannerId);

    //Invoked when an observer requests a VIN, returned through notifyVIN() method
    void requestVIN();


}

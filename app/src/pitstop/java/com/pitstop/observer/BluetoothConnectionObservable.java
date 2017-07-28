package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.models.ReadyDevice;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObservable extends Subject{

    interface State{
        final String DISCONNECTED = "disconnected"; //No bluetooth activity
        final String SEARCHING = "state_searching"; //Searching for bluetooth device
        final String VERIFYING = "state_verifying"; //Verifying currently connected device
        final String CONNECTED = "state_connected"; //Established trusted connection with device
    }

    //Invoked if device recognized as broken and requires id overwrite
    void notifyDeviceNeedsOverwrite();

    //Invoked if observable is in the process of searching for a device
    void notifySearchingForDevice();

    //Invoked if device has been successfully paired and is ready for use, this includes
    //  syncing rtc time
    void notifyDeviceReady(String vin, String scannerId, String scannerName);

    //Invoked if the currently used scanner has disconnected
    void notifyDeviceDisconnected();

    //Invoked when the device is connected and now being verified
    void notifyVerifyingDevice();

    //Invoked when the device is verified and now being synced
    void notifySyncingDevice();

    //Invoked if dtc data has been received from the device
    void notifyDtcData(DtcPackage dtcPackage);

    void notifyVin(String vin);

    //Invoked when a observer needs the dtc data
    void requestDtcData();

    //Invoked when an observer needs the device VIN
    void requestVin();

    //Request scan for device
    void requestDeviceSearch();

    //Returns the current state of connection with a device, DISCONNECTED if none
    String getDeviceState();

    //Returns the already connected device, NULL if not connected
    ReadyDevice getReadyDevice();

}

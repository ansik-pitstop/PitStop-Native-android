package com.pitstop.observer;

import com.pitstop.models.ReadyDevice;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObservable extends Subject{

    interface State{
        String DISCONNECTED = "disconnected"; //No bluetooth activity
        String SEARCHING = "state_searching"; //Searching for bluetooth device
        String VERIFYING = "state_verifying"; //Verifying currently connected device
        String CONNECTED = "state_connected"; //Established trusted connection with device
    }

    //Invoked when a observer needs the dtc data
    void requestDtcData();

    //Invoked when an observer needs the device VIN
    void requestVin();

    //Invoked when an observers wants to turn the historical data OFF/ON
    void requestHistoricalDataStateChange(boolean on);

    //Invoked when an observer wants to retrieve all the supported pids along with their values
    void requestAllPid();

    //Request scan for device
    void requestDeviceSearch(boolean urgent, boolean ignoreVerification);

    //Returns the current state of connection with a device, DISCONNECTED if none
    String getDeviceState();

    //Returns the already connected device, NULL if not connected
    ReadyDevice getReadyDevice();

}

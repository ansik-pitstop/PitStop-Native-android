package com.pitstop.observer;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.ReadyDevice;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObservable extends Subject{

    //Number of seconds before an error or success response occurs with pid data
    double RETRIEVAL_LEN_ALL_PID = (BluetoothAutoConnectService.PID_RETRY_LEN
            * BluetoothAutoConnectService.PID_RETRY_COUNT)
            + BluetoothAutoConnectService.DTC_RETRY_LEN;

    //Number of seconds before an error or success response occurs with dtc data
    double RETRIEVAL_LEN_DTC = BluetoothAutoConnectService.DTC_RETRY_COUNT
            * BluetoothAutoConnectService.DTC_RETRY_LEN
            + BluetoothAutoConnectService.DTC_RETRY_LEN;

    void disconnect();

    interface State{
        String DISCONNECTED = "disconnected"; //No bluetooth activity
        String SEARCHING = "state_searching"; //Searching for bluetooth device
        String FOUND_DEVICES = "found devices";
        String CONNECTING = "state_connecting"; //Connecting to bluetooth device
        String CONNECTED_UNVERIFIED = "state_connected_unverified"; //Established not yet trusted connection
        String VERIFYING = "state_verifying"; //Verifying currently connected device
        String CONNECTED_VERIFIED = "state_connected_verified"; //Established trusted connection with device
    }

    boolean requestPidInitialization();

    //Invoked when a observer needs the dtc data
    boolean requestDtcData();

    //Invoked when an observer needs the device VIN
    boolean requestVin();

    //Invoked when an observer wants to retrieve all the supported pids along with their values
    boolean requestAllPid();

    //Invoked when an observer wants to retrieve the device rtc time
    boolean requestDeviceTime();

    void getSupportedPids();

    //Request scan for device
    void requestDeviceSearch(boolean urgent, boolean ignoreVerification);

    //Returns the current state of connection with a device, DISCONNECTED if none
    String getDeviceState();

    //Returns the already connected device, NULL if not connected
    ReadyDevice getReadyDevice();

}

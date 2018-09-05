package com.pitstop.observer;

import com.continental.rvd.mobile_sdk.EBindingQuestionType;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.models.ReadyDevice;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothConnectionObservable extends Subject{

    //Number of seconds before an error or success response occurs with pid data
    double RETRIEVAL_LEN_ALL_PID = (BluetoothService.PID_RETRY_LEN
            * BluetoothService.PID_RETRY_COUNT)
            + BluetoothService.DTC_RETRY_LEN;

    //Number of seconds before an error or success response occurs with dtc data
    double RETRIEVAL_LEN_DTC = BluetoothService.DTC_RETRY_COUNT
            * BluetoothService.DTC_RETRY_LEN
            + BluetoothService.DTC_RETRY_LEN;

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

    boolean answerBindingQuestion(EBindingQuestionType questionType, String answer);

    boolean requestPidInitialization();

    //Invoked when a observer needs the dtc data
    boolean requestDtcData();

    //Invoked when an observer needs the device VIN
    boolean requestVin();

    //Invoked when an observer wants to retrieve all the supported pids along with their values
    boolean requestAllPid();

    //Invoked when an observer wants to retrieve the device rtc time
    boolean requestDeviceTime();

    //Describe protocol, only works for ELM327 devices
    boolean requestDescribeProtocol();

    //Get 2141 Emissions PID
    boolean request2141PID();

    boolean requestStoredDTC();

    boolean requestPendingDTC();

    void getSupportedPids();

    //Request scan for device
    boolean requestDeviceSearch(boolean urgent, boolean ignoreVerification
            , BluetoothDeviceManager.DeviceType deviceType);

    //Returns the current state of connection with a device, DISCONNECTED if none
    String getDeviceState();

    //Returns the already connected device, NULL if not connected
    ReadyDevice getReadyDevice();

}

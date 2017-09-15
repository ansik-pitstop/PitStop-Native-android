package com.pitstop.bluetooth.handler;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public interface BluetoothDataHandlerManager{
    
    //Methods below are invoked by all data handlers
    boolean isDeviceVerified();
    void trackBluetoothEvent(String event, String scannerId, String vin);
    void trackBluetoothEvent(String event);

    //Methods below are invoked by Trip data handler
    long getRtcTime();
    boolean isConnectedTo215();

    //Methods below are invoked by PID data handler
    void setPidsToBeSent(String pids);

    //Methods below are invoked by RTC data handler
    void requestDeviceSync();

    //Methods below are invoked by DTC data handler
    void requestFreezeData();

    //Methods below are invoked by VIN data handler
    void onHandlerVerifyingDevice();
    void onHandlerReadVin(String vin);
}

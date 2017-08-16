package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public interface DeviceVerificationObserver {
    void onVerificationSuccess(String vin, String deviceId);
    void onVerificationDeviceBrokenAndCarMissingScanner(String vin, String deviceId);
    void onVerificationDeviceBrokenAndCarHasScanner(String vin, String deviceId);
    void onVerificationDeviceInvalid();
    void onVerificationDeviceAlreadyActive();
    void onVerificationError();
}

package com.pitstop.bluetooth;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public interface DeviceVerificationObserver {
    void onVerificationSuccess();
    void onVerificationDeviceBrokenAndCarMissingScanner();
    void onVerificationDeviceBrokenAndCarHasScanner(String scannerId);
    void onVerificationDeviceInvalid();
    void onVerificationDeviceAlreadyActive();
    void onVerificationError();
}

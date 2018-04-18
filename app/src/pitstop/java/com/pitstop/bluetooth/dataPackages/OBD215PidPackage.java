package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

public class OBD215PidPackage extends CastelPidPackage {

    public OBD215PidPackage(String deviceId, String rtcTime, String mileage) {
        super(deviceId,rtcTime,mileage);
    }

    public OBD215PidPackage(OBD215PidPackage o) {
        this(o.getDeviceId(),o.getRtcTime(),o.getMileage());
    }
}

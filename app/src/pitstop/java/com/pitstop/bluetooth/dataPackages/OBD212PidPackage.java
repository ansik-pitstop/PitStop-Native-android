package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

public class OBD212PidPackage extends CastelPidPackage {

    public OBD212PidPackage(String deviceId, String rtcTime, String mileage) {
        super(deviceId,mileage,rtcTime);
    }

    public OBD212PidPackage(OBD212PidPackage o){
        this(o.getDeviceId(),o.getRtcTime(),o.getMileage());
    }

}

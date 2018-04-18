package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

public abstract class CastelPidPackage extends PidPackage {

    private String rtcTime;
    private String mileage;

    public CastelPidPackage(String deviceId, String mileage, String rtcTime) {
        super(deviceId);
        this.mileage = mileage;
        this.rtcTime = rtcTime;
    }

    public CastelPidPackage(CastelPidPackage o){
        this(o.getDeviceId(),o.getMileage(),o.getRtcTime());
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public String getMileage() {
        return mileage;
    }
}

package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

public abstract class CastelPidPackage extends PidPackage {

    private String tripId;
    private String rtcTime;
    private String mileage;

    public CastelPidPackage(String deviceId, String tripId, String mileage, String rtcTime) {
        super(deviceId);
        this.tripId = tripId;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
    }

    public CastelPidPackage(CastelPidPackage o){
        this(o.getDeviceId(),o.getTripId(),o.getMileage(),o.getRtcTime());
    }

    public String getTripId() {
        return tripId;
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public String getMileage() {
        return mileage;
    }
}

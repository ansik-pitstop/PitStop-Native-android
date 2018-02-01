package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

//real time
//trip id
//trip mileage
public class OBD212PidPackage extends CastelPidPackage {

    private boolean realTime;

    public OBD212PidPackage(String deviceId, String rtcTime, boolean realTime
            , String tripId, String mileage) {
        super(deviceId,tripId,mileage,rtcTime);
        this.realTime = realTime;
    }

    public OBD212PidPackage(OBD212PidPackage o){
        this(o.getDeviceId(),o.getRtcTime(),o.realTime,o.getTripId(),o.getMileage());
    }

    public boolean isRealTime() {
        return realTime;
    }

}

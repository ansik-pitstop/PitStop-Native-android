package com.pitstop.bluetooth.dataPackages;

import java.util.HashMap;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class PidPackage {
    public String deviceId;
    public String tripId;
    public String tripMileage;
    public String rtcTime;
    public String timestamp;
    public boolean realTime;
    public HashMap<String, String> pids; // key is pid type, value is value

    public PidPackage() {
    }

    public PidPackage(PidPackage pidPackage) {
        this.deviceId = pidPackage.deviceId;
        this.tripId = pidPackage.tripId;
        this.tripMileage = pidPackage.tripMileage;
        this.rtcTime = pidPackage.rtcTime;
        this.timestamp = pidPackage.timestamp;
        this.pids = pidPackage.pids;
    }

    public void setPids(HashMap<String, String> newpids){
        this.pids = newpids;
    }

    @Override
    public String toString() {
        return "PidPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", tripMileage='" + tripMileage + '\'' +
                ", rtcTime='" + rtcTime + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", pids=" + pids +
                '}';
    }
}

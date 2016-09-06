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
    public HashMap<String, String> pids; // key is pid type, value is value

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

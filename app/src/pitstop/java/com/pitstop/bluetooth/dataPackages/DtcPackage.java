package com.pitstop.bluetooth.dataPackages;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class DtcPackage {
    public String deviceId;
    public Map<String,Boolean> dtcs;
    public String rtcTime;

    public DtcPackage(String deviceId, String rtcTime, Map<String,Boolean> dtcs){
        this.deviceId = deviceId;
        this.rtcTime = rtcTime;
        this.dtcs = dtcs;
    }

    public DtcPackage(){
        dtcs = new HashMap<>();
        rtcTime = "";
        deviceId = "";
    }

    @Override
    public String toString() {
        return "DtcPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", dtcs=" + dtcs +
                ", rtcTime='" + rtcTime + '\'' +
                '}';
    }
}

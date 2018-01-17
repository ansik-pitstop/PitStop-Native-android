package com.pitstop.bluetooth.dataPackages;

import java.util.Map;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class DtcPackage {
    public String deviceId;
    public Map<String,Boolean> dtcs;
    public String rtcTime;

    @Override
    public String toString() {
        return "DtcPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", dtcs=" + dtcs +
                ", rtcTime='" + rtcTime + '\'' +
                '}';
    }
}

package com.pitstop.bluetooth.dataPackages;

import java.util.Map;

/**
 * Created by yifan on 16/12/5.
 */

public class FreezeFramePackage {

    public String deviceId;
    public long rtcTime;
    public Map<String, String> freezeData;

    @Override
    public String toString() {
        return "FreezeFramePackage{" +
                "deviceId='" + deviceId + "'" +
                ", rtcTime=" + rtcTime +
                ", freezeData=" + freezeData;
    }
}

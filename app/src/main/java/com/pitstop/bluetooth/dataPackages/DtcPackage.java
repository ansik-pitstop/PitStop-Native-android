package com.pitstop.bluetooth.dataPackages;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class DtcPackage {
    public String deviceId;
    public boolean isPending;
    public int dtcNumber; // TODO: remove?
    public String[] dtcs;
    public String rtcTime;

    @Override
    public String toString() {
        return "DtcPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", isPending=" + isPending +
                ", dtcNumber=" + dtcNumber +
                ", dtcs=" + Arrays.toString(dtcs) +
                ", rtcTime='" + rtcTime + '\'' +
                '}';
    }
}

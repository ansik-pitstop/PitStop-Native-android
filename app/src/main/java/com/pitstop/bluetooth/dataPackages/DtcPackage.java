package com.pitstop.bluetooth.dataPackages;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class DtcPackage {
    public String deviceId;
    public boolean isPending;
    public int dtcNumber;
    public String[] dtcs;
    public String rtcTime;

    public Set<String> getDtcAsSet(){

        Set<String> dtcSet = new HashSet<>();
        if (dtcs != null){
            Collections.addAll(dtcSet, dtcs);
        }
        return dtcSet;

    }

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

package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public class ParameterPackage {
    public enum ParamType {
        VIN, RTC_TIME, SUPPORTED_PIDS
    }

    public ParamType paramType;
    public boolean success;
    public String deviceId;
    public String value;

    @Override
    public String toString() {
        return "ParameterPackage{" +
                "paramType=" + paramType +
                ", success=" + success +
                ", deviceId='" + deviceId + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

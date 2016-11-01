package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Ben Wu on 2016-09-03.
 */
public class TripInfoPackage {
    public enum TripFlag {
        START, END, UPDATE
    }

    public String deviceId;
    public int tripId; // device specific
    public double mileage; // in kilometres
    public long rtcTime; // unix time in seconds
    public TripFlag flag;

    @Override
    public String toString() {
        return "TripInfoPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", tripId=" + tripId +
                ", mileage=" + mileage +
                ", rtcTime=" + rtcTime +
                ", flag=" + flag +
                '}';
    }
}

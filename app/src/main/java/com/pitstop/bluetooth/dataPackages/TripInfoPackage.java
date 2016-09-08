package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Ben Wu on 2016-09-03.
 */
public class TripInfoPackage {
    public enum TripFlag {
        START, END, UPDATE
    }

    public String deviceId;
    public long tripId; // device specific
    public double mileage; // in kilometres
    public double rtcTime; // unix time in seconds
    public TripFlag flag;
}

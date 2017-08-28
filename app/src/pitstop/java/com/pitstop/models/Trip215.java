package com.pitstop.models;

/**
 * Stores data for the 215 device trip, can be used for Trip Start and Trip End
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public abstract class Trip215 {

    private double mileage;
    private long rtcTime;
    private String scannerName;
    private long tripIdRaw = -1;

    public Trip215(double mileage, long rtcTime, String scannerName, long tripIdRaw) {
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
        this.tripIdRaw = tripIdRaw;
    }

    public double getMileage() {
        return mileage;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    public long getRtcTime() {
        return rtcTime;
    }

    public void setRtcTime(long rtcTime) {
        this.rtcTime = rtcTime;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }

    public long getTripIdRaw() {
        return tripIdRaw;
    }

    public void setTripIdRaw(long tripIdRaw) {
        this.tripIdRaw = tripIdRaw;
    }
}

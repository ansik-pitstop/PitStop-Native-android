package com.pitstop.models;

/**
 * Stores data for the 215 device trip, can be used for Trip Start and Trip End
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215 {
    private int tripId;
    private double mileage;
    private long rtcTime;
    private String scannerName;

    public Trip215(int id, double mileage, long rtcTime, String scannerName) {
        this.tripId = id;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
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
}

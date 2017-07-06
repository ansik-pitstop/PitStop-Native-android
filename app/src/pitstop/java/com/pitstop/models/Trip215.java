package com.pitstop.models;

/**
 * Stores data for the 215 device trip, can be used for Trip Start and Trip End
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215 {
    private int tripId;
    private int mileage;
    private int rtcTime;
    private String scannerName;

    public Trip215(int id, int mileage, int rtcTime, String scannerName) {
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

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public int getRtcTime() {
        return rtcTime;
    }

    public void setRtcTime(int rtcTime) {
        this.rtcTime = rtcTime;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }
}

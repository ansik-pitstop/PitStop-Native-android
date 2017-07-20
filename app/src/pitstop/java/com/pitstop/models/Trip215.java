package com.pitstop.models;

/**
 * Stores data for the 215 device trip, can be used for Trip Start and Trip End
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215 {
    private int tripId = -1;
    private double mileage;
    private long rtcTime;
    private String scannerName;
    private long tripIdRaw = -1;

    public Trip215(int tripIdRaw, double mileage, long rtcTime, String scannerName) {
        this.tripIdRaw = tripIdRaw;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
    }

    public Trip215(int id, long tripIdRaw, double mileage, long rtcTime, String scannerName) {
        this.tripId = id;
        this.tripIdRaw = tripIdRaw;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
    }

    public long getTripIdRaw() {
        return tripIdRaw;
    }

    public void setTripIdRaw(long tripIdRaw) {
        this.tripIdRaw = tripIdRaw;
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

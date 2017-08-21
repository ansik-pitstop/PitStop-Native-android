package com.pitstop.models;

/**
 * Stores data for the 215 device trip, can be used for Trip Start and Trip End
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215 {

    public static final String TRIP_START = "trip_start";
    public static final String TRIP_END = "trip_end";

    private int tripId = -1;
    private double mileage;
    private long rtcTime;
    private String scannerName;
    private long tripIdRaw = -1;
    private String type;

    public Trip215(String type, int tripIdRaw, double mileage, long rtcTime, String scannerName) {
        if (!type.equals(TRIP_START) && !type.equals(TRIP_END))
            throw new IllegalArgumentException();
        this.type = type;
        this.tripIdRaw = tripIdRaw;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
    }

    public Trip215(String type, int id, long tripIdRaw, double mileage, long rtcTime
            , String scannerName) {
        if (!type.equals(TRIP_START) && !type.equals(TRIP_END))
            throw new IllegalArgumentException();
        this.type = type;
        this.tripId = id;
        this.tripIdRaw = tripIdRaw;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.scannerName = scannerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString(){
        return type+", tripIdRaw:"+tripIdRaw+", scannerName: "+scannerName+", mileage: "+mileage
                +", rtcTime: "+rtcTime;
    }
}

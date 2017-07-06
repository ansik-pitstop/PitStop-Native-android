package com.pitstop.models;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215 {
    private int id;
    private int mileage;
    private int rtcTime;
    private String vin;

    public Trip215(int id, int mileage, int rtcTime, String vin) {
        this.id = id;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.vin = vin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
}

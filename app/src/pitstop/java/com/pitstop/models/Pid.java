package com.pitstop.models;

import com.google.gson.annotations.Expose;

/**
 * Created by Paul Soladoye  on 3/16/2016.
 */
public class Pid {
    @Expose(serialize = false, deserialize = false)
    private int id;
    private String dataNumber;
    private String rtcTime;
    private String timeStamp;
    private long tripIdRaw;
    private String pids;
    private double mileage;
    private double calculatedMileage;
    private String deviceId;
    private int tripId;

    public Pid() {}

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public int getTripId() {
        return tripId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public void setRtcTime(String rtcTime) {
        this.rtcTime = rtcTime;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTripIdRaw() {
        return tripIdRaw;
    }

    public void setTripIdRaw(long tripIdRaw) {
        this.tripIdRaw = tripIdRaw;
    }

    public String getPids() {
        return pids;
    }

    public void setPids(String pids) {
        this.pids = pids;
    }

    public String getDataNumber() {
        return dataNumber;
    }

    public void setDataNumber(String dataNumber) {
        this.dataNumber = dataNumber;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    public double getMileage() {
        return mileage;
    }

    public void setCalculatedMileage(double mileage) {
        this.calculatedMileage = mileage;
    }

    public double getCalculatedMileage() {
        return calculatedMileage;
    }

    public String toString() {
        return "dataNumber: " + dataNumber + ", rtcTime: " + rtcTime + ", timeStamp: " + timeStamp + ", tripIdRaw: " + tripIdRaw
                + ", mileage: " + mileage + ", calculatedMileage: " + calculatedMileage + ", pids: " + pids;
    }
}

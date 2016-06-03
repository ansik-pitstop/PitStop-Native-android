package com.pitstop.DataAccessLayer.DTOs;

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
    private String pids;
    private String mileage;

    public Pid() {}

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

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }
}

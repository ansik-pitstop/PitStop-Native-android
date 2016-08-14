package com.pitstop.model;

import com.castel.obd.info.PIDInfo;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by Ben Wu on 2016-07-05.
 */

public class Dtc { // for saving dtcs locally when no internet connection
    int carId;
    double mileage;
    String rtcTime;
    String dtcCode;
    boolean isPending;
    List<PIDInfo> freezeData;

    public Dtc(int carId, double mileage, String rtcTime, String dtcCode, boolean isPending, List<PIDInfo> freezeData) {
        this.carId = carId;
        this.mileage = mileage;
        this.rtcTime = rtcTime;
        this.dtcCode = dtcCode;
        this.isPending = isPending;
        this.freezeData = freezeData;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public double getMileage() {
        return mileage;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public void setRtcTime(String rtcTime) {
        this.rtcTime = rtcTime;
    }

    public String getDtcCode() {
        return dtcCode;
    }

    public void setDtcCode(String dtcCode) {
        this.dtcCode = dtcCode;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public List<PIDInfo> getFreezeData() {
        return freezeData;
    }

    public void setFreezeData(List<PIDInfo> freezeData) {
        this.freezeData = freezeData;
    }
}

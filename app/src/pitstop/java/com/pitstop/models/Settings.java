package com.pitstop.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Not complete, doesn't include everything stored inside settings
 *
 * Created by Karol Zdebel on 7/17/2017.
 */

public class Settings {

    private int userId;
    @SerializedName("mainCar")
    @Expose
    private int carId = -1;  //User settings car id
    @SerializedName("isFirstCarAdded")
    @Expose
    private boolean firstCarAdded;  //Whether user ever added a car
    @SerializedName("alarmsEnabled")
    @Expose
    private boolean alarmsEnabled;

    @SerializedName("odometer")
    @Expose
    private String odometer;
    @SerializedName("timezone")
    @Expose
    private String timezone;

    public Settings(int userId, int carId, boolean firstCarAdded, boolean alarms, String odometer, String timezone) {
        this.userId = userId;
        this.carId = carId;
        this.firstCarAdded = firstCarAdded;
        this.alarmsEnabled  = alarms;
        this.odometer = odometer;
        this.timezone = timezone;
    }

    public boolean hasMainCar(){
        return carId > 0;
    }

    public int getCarId() {
        return carId;
    }

    public boolean isFirstCarAdded() {
        return firstCarAdded;
    }

    public void setFirstCarAdded(boolean firstCarAdded){
        this.firstCarAdded = firstCarAdded;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getOdometer() { return odometer; }

    public void setOdometer(String odometer) { this.odometer = odometer; }

    public boolean isAlarmsEnabled() {return alarmsEnabled;}

    public void setAlarmsEnabled(boolean alarmsEnabled) {this.alarmsEnabled = alarmsEnabled;}

    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getTimezone() { return timezone; }

    public String toString(){
        return "{ userID: " + this.userId +
                 ", carID: " + this.carId +
                ", firstCarAdded " + Boolean.toString(this.firstCarAdded) +
                ", alarmsEnabled " + Boolean.toString(this.alarmsEnabled) + "}";

    }
}

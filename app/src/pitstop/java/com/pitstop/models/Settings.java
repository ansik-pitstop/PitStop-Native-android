package com.pitstop.models;

/**
 * Not complete, doesn't include everything stored inside settings
 *
 * Created by Karol Zdebel on 7/17/2017.
 */

public class Settings {

    private int userId;
    private int carId = -1;  //User settings car id
    private boolean firstCarAdded;  //Whether user ever added a car
    private boolean alarmsEnabled;

    public Settings(int userId, int carId, boolean firstCarAdded, boolean alarms) {
        this.userId = userId;
        this.carId = carId;
        this.firstCarAdded = firstCarAdded;
        this.alarmsEnabled  = alarms;
    }

    public boolean hasMainCar(){
        return carId != -1;
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

    public boolean isAlarmsEnabled() {return alarmsEnabled;}

    public void setAlarmsEnabled(boolean alarmsEnabled) {this.alarmsEnabled = alarmsEnabled;}

    public String toString(){
        return "{ userID: " + this.userId +
                 ", carID: " + this.carId +
                ", firstCarAdded " + Boolean.toString(this.firstCarAdded) +
                ", alarmsEnabled " + Boolean.toString(this.alarmsEnabled) + "}";

    }
}

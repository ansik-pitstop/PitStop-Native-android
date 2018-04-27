package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 13/3/18.
 */

public class Location {

    private int realId; // PK

    @SerializedName("id")
    @Expose
    private String typeId;

    @SerializedName("data")
    @Expose
    private String data;

    private int locationPolylineId; // FK

    private String tripId; // FK

    private String carVin; // FK

    public Location() {
    }

    public Location(String typeId, String data) {
        this.typeId = typeId;
        this.data = data;
    }

    public Location(int realId, String typeId, String data, int locationPolylineId, String tripId, String carVin) {
        this.realId = realId;
        this.typeId = typeId;
        this.data = data;
        this.locationPolylineId = locationPolylineId;
        this.tripId = tripId;
        this.carVin = carVin;
    }

    public int getRealId() {
        return realId;
    }

    public void setRealId(int realId) {
        this.realId = realId;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getLocationPolylineId() {
        return locationPolylineId;
    }

    public void setLocationPolylineId(int locationPolylineId) {
        this.locationPolylineId = locationPolylineId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getCarVin() {
        return carVin;
    }

    public void setCarVin(String carVin) {
        this.carVin = carVin;
    }

    @Override
    public String toString(){
        return data;
    }
}
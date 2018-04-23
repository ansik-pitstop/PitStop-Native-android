package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by David C. on 13/3/18.
 */

public class LocationPolyline {

    private int id; // PK

    @SerializedName("location")
    @Expose
    private List<Location> location;

    @SerializedName("phoneTimestamp")
    @Expose
    private String timestamp;

    private String tripId; // FK

    private String carVin; // FK

    public LocationPolyline() {
    }

    public LocationPolyline(List<Location> location, String timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public LocationPolyline(int id, List<Location> location, String timestamp, String tripId, String carVin) {
        this.id = id;
        this.location = location;
        this.timestamp = timestamp;
        this.tripId = tripId;
        this.carVin = carVin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Location> getLocation() {
        return location;
    }

    public void setLocation(List<Location> location) {
        this.location = location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
}

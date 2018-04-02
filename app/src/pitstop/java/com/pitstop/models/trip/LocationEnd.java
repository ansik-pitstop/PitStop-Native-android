package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 9/3/18.
 * <p>
 * Temporal. Should be replaced by Location2 object
 */

public class LocationEnd {

    private int id; // PK

    @SerializedName("altitude")
    @Expose
    private String altitude;

    @SerializedName("latitude")
    @Expose
    private String latitude;

    @SerializedName("longitude")
    @Expose
    private String longitude;

    @SerializedName("endLocation")
    @Expose
    private String endLocation;

    @SerializedName("endCityLocation")
    @Expose
    private String endCityLocation;

    @SerializedName("endStreetLocation")
    @Expose
    private String endStreetLocation;

    private String tripId; // FP

    private String carVin; // FK

    public LocationEnd() {
    }

    public LocationEnd(String altitude, String latitude, String longitude, String endLocation, String endCityLocation, String endStreetLocation) {
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endLocation = endLocation;
        this.endCityLocation = endCityLocation;
        this.endStreetLocation = endStreetLocation;
    }

    public LocationEnd(int id, String altitude, String latitude, String longitude, String endLocation, String endCityLocation, String endStreetLocation, String tripId, String carVin) {
        this.id = id;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endLocation = endLocation;
        this.endCityLocation = endCityLocation;
        this.endStreetLocation = endStreetLocation;
        this.tripId = tripId;
        this.carVin = carVin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }

    public String getEndCityLocation() {
        return endCityLocation;
    }

    public void setEndCityLocation(String endCityLocation) {
        this.endCityLocation = endCityLocation;
    }

    public String getEndStreetLocation() {
        return endStreetLocation;
    }

    public void setEndStreetLocation(String endStreetLocation) {
        this.endStreetLocation = endStreetLocation;
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

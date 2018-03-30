package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * Created by David C. on 9/3/18.
 *
 * Temporal. Should be replaced by Location2 object
 */

public class LocationStart extends SugarRecord {

    private String tripId;

    @SerializedName("altitude")
    @Expose
    private String altitude;

    @SerializedName("latitude")
    @Expose
    private String latitude;

    @SerializedName("longitude")
    @Expose
    private String longitude;

    @SerializedName("startLocation")
    @Expose
    private String startLocation;

    @SerializedName("startCityLocation")
    @Expose
    private String startCityLocation;

    @SerializedName("startStreetLocation")
    @Expose
    private String startStreetLocation;

    public LocationStart() {
    }

    public LocationStart(String altitude, String latitude, String longitude, String startLocation, String startCityLocation, String startStreetLocation) {
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startLocation = startLocation;
        this.startCityLocation = startCityLocation;
        this.startStreetLocation = startStreetLocation;
    }

    public LocationStart(String tripId, String altitude, String latitude, String longitude, String startLocation, String startCityLocation, String startStreetLocation) {
        this.tripId = tripId;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startLocation = startLocation;
        this.startCityLocation = startCityLocation;
        this.startStreetLocation = startStreetLocation;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
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

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getStartCityLocation() {
        return startCityLocation;
    }

    public void setStartCityLocation(String startCityLocation) {
        this.startCityLocation = startCityLocation;
    }

    public String getStartStreetLocation() {
        return startStreetLocation;
    }

    public void setStartStreetLocation(String startStreetLocation) {
        this.startStreetLocation = startStreetLocation;
    }
}

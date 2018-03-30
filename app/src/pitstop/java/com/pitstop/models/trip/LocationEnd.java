package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * Created by David C. on 9/3/18.
 *
 * Temporal. Should be replaced by Location2 object
 */

public class LocationEnd extends SugarRecord {

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

    @SerializedName("endLocation")
    @Expose
    private String endLocation;

    @SerializedName("endCityLocation")
    @Expose
    private String endCityLocation;

    @SerializedName("endStreetLocation")
    @Expose
    private String endStreetLocation;

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

    public LocationEnd(String tripId, String altitude, String latitude, String longitude, String endLocation, String endCityLocation, String endStreetLocation) {
        this.tripId = tripId;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endLocation = endLocation;
        this.endCityLocation = endCityLocation;
        this.endStreetLocation = endStreetLocation;
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
}

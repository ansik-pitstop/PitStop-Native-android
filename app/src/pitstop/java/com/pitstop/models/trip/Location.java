package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 9/3/18.
 */

public class Location {

    @SerializedName("altitude")
    @Expose
    private int altitude;
    @SerializedName("latitude")
    @Expose
    private int latitude;
    @SerializedName("longitude")
    @Expose
    private int longitude;

    public Location() {
        this.altitude = 0;
        this.latitude = 0;
        this.longitude = 0;
    }

    public Location(int altitude, int latitude, int longitude) {
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public int getLatitude() {
        return latitude;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

}

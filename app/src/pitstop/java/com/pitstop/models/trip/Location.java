package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by David C. on 9/3/18.
 */

@Entity
public class Location {

    @Id(autoincrement = true)
    private long id;

    @NotNull
    @SerializedName("altitude")
    @Expose
    private double altitude;
    @NotNull
    @SerializedName("latitude")
    @Expose
    private double latitude;
    @NotNull
    @SerializedName("longitude")
    @Expose
    private double longitude;
    @NotNull
    @SerializedName("timestamp")
    @Expose
    private long timestamp;

    @Generated(hash = 602334664)
    public Location(long id, double altitude, double latitude, double longitude,
            long timestamp) {
        this.id = id;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
    @Generated(hash = 375979639)
    public Location() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public double getAltitude() {
        return this.altitude;
    }
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
    public double getLatitude() {
        return this.latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return this.longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}

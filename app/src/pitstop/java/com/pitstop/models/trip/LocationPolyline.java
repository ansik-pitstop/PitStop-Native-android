package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.List;

/**
 * Created by David C. on 13/3/18.
 */

public class LocationPolyline extends SugarRecord {

    private String tripId;

    @Ignore
    @SerializedName("location")
    @Expose
    private List<Location> location;

    // This will be the ID
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    public LocationPolyline() {
    }

    public LocationPolyline(List<Location> location, String timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public LocationPolyline(String tripId, List<Location> location, String timestamp) {
        this.tripId = tripId;
        this.location = location;
        this.timestamp = timestamp;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
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
}

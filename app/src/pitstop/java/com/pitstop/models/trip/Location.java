package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * Created by David C. on 13/3/18.
 */

public class Location extends SugarRecord {

    private int locationPolylineId;

    @SerializedName("id")
    @Expose
    private String typeId;

    @SerializedName("data")
    @Expose
    private String data;

    public Location() {
    }

    public Location(String typeId, String data) {
        this.typeId = typeId;
        this.data = data;
    }

    public Location(int locationPolylineId, String typeId, String data) {
        this.locationPolylineId = locationPolylineId;
        this.typeId = typeId;
        this.data = data;
    }

    public int getLocationPolylineId() {
        return locationPolylineId;
    }

    public void setLocationPolylineId(int locationPolylineId) {
        this.locationPolylineId = locationPolylineId;
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
}
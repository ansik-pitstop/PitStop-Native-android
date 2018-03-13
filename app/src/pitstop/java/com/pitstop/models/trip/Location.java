package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by David C. on 13/3/18.
 */

@Entity
public class Location {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("data")
    @Expose
    private String data;
    private long locationId; // referencedJoinProperty from LOCATIONPOLYLINE object

    @Generated(hash = 1845230654)
    public Location(String id, String data, long locationId) {
        this.id = id;
        this.data = data;
        this.locationId = locationId;
    }
    @Generated(hash = 375979639)
    public Location() {
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getData() {
        return this.data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public long getLocationId() {
        return this.locationId;
    }
    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

}

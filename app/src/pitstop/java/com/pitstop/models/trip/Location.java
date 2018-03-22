package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by David C. on 13/3/18.
 */

@Entity
public class Location {

    @Id(autoincrement = true)
    private Long objId;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("data")
    @Expose
    private String data;
    @NotNull
    private String locationPolylineId;

    @Generated(hash = 1005771765)
    public Location(Long objId, String id, String data,
            @NotNull String locationPolylineId) {
        this.objId = objId;
        this.id = id;
        this.data = data;
        this.locationPolylineId = locationPolylineId;
    }
    @Generated(hash = 375979639)
    public Location() {
    }
    public Long getObjId() {
        return this.objId;
    }
    public void setObjId(Long objId) {
        this.objId = objId;
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
    public String getLocationPolylineId() {
        return this.locationPolylineId;
    }
    public void setLocationPolylineId(String locationPolylineId) {
        this.locationPolylineId = locationPolylineId;
    }

}

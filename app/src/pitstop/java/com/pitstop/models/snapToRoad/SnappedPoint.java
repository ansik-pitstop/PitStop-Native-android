package com.pitstop.models.snapToRoad;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 16/3/18.
 */

public class SnappedPoint {

    @SerializedName("location")
    @Expose
    private SnappedLocation location;
    @SerializedName("originalIndex")
    @Expose
    private Integer originalIndex;
    @SerializedName("placeId")
    @Expose
    private String placeId;

    public SnappedLocation getLocation() {
        return location;
    }

    public void setLocation(SnappedLocation location) {
        this.location = location;
    }

    public Integer getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(Integer originalIndex) {
        this.originalIndex = originalIndex;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    @Override
    public String toString(){
        try{
            return String.format("[%d](%f,%f)",originalIndex,location.getLatitude(),location.getLongitude());
        }catch(NullPointerException e){
            return "null";
        }
    }

}

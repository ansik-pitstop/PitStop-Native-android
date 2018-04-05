package com.pitstop.models.snapToRoad;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 16/3/18.
 */

public class SnappedLocation {

    @SerializedName("latitude")
    @Expose
    private Float latitude;
    @SerializedName("longitude")
    @Expose
    private Float longitude;

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString(){
        try{
            return String.format("(%f,%f)",latitude,longitude);
        }catch(NullPointerException e){
            return "null";
        }
    }

}

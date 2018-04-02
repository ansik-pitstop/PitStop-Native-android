package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 9/3/18.
 */

public class Location2 {

    private long id;

    private Trip trip;

    @SerializedName("altitude")
    @Expose
    private double altitude;
    @SerializedName("latitude")
    @Expose
    private double latitude;
    @SerializedName("longitude")
    @Expose
    private double longitude;
    @SerializedName("timestamp")
    @Expose
    private long timestamp;

}

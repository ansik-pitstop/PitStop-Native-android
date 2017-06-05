package com.pitstop.models;

import android.location.Location;

/**
 * Created by Matthew on 2017-05-23.
 */

public class TripLocation {
    private double latitude;
    private double longitude;

    private long time;


   public TripLocation(Location location){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        time = location.getTime();

    }
    public double getLatitude(){
        return latitude;
    }
    public double getLongitude(){
        return longitude;
    }
    public long getTime(){
        return time;
    }

    public void setTime(Long time){
        this.time = time;
    }
}

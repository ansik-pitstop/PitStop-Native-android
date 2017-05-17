package com.pitstop.models;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthew on 2017-05-15.
 */

public class Trip  {

    private Location start;
    private Location end;
    private String startAddress;
    private String endAddress;
    private double totalDistance;
    private int tripId;
    private List<Location> path = new ArrayList<>();

    public void setStart(Location start){
        this.start = start;
    }
    public void setEnd(Location end){
        this.end = end;
    }

    public void setStartAddress(String address){
        this.startAddress = address;
    }

    public void setEndAddress(String address){
        this.endAddress = address;
    }

    public void setTotalDistance(double totalDistance){
        this.totalDistance = totalDistance;
    }

    public void setTripId(int tripId){
        this.tripId = tripId;
    }
    public void setPath(List<Location> path){
        this.path = path;
    }


    public void addPoint(Location location){
        path.add(location);
    }

    public List<Location> getPath(){return path;}

    public Location getEnd(){return end;}

    public Location getStart(){return start;}

    public String getStartAddress(){return startAddress;}

    public String getEndAddress(){return endAddress;}


    public double getTotalDistance(){return totalDistance;}

    public int getId(){return tripId;}

    public void reset(){
        start = null;
        end = null;
        tripId = 0;// might have to be careful with this
        totalDistance = 0;
        path.clear();
    }


}

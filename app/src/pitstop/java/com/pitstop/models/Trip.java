package com.pitstop.models;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthew on 2017-05-15.
 */

public class Trip  {

    private TripLocation start;
    private TripLocation end;
    private String startAddress;
    private String endAddress;
    private double totalDistance = 0;
    private int tripId;
    private List<TripLocation> path = new ArrayList<>();

    public void setStart(TripLocation start){
        this.start = start;
    }
    public void setEnd(TripLocation end){
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
    public void setPath(List<TripLocation> path){
        this.path = path;
    }


    public void addPoint(TripLocation location){
        path.add(location);
    }

    public List<TripLocation> getPath(){return path;}

    public TripLocation getEnd(){return end;}

    public TripLocation getStart(){return start;}

    public String getStartAddress(){return startAddress;}

    public String getEndAddress(){return endAddress;}


    public double getTotalDistance(){return totalDistance;}

    public int getId(){return tripId;}

    public void addDist(double dist){
        totalDistance += dist;
    }

    public void reset(){
        start = null;
        end = null;
        tripId = 0;// might have to be careful with this
        totalDistance = 0;
        path.clear();
    }


}

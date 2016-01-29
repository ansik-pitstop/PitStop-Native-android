package com.pitstop.database.models;

import com.pitstop.database.DBModel;

import java.io.Serializable;

/**
 * Created by David Liu on 11/20/2015.
 */
public class Cars extends DBModel{
    public Cars() {
        super("Cars", "CarID", "owner");
    }

    protected void setUpTable(){
        columns.put("CarID","Text PRIMARY KEY");
        columns.put("owner","Text");
        columns.put("scannerId","Text");
        columns.put("VIN","Text");
        columns.put("baseMileage","Text");
        columns.put("cityMileage","Text");
        columns.put("highwayMileage","Text");
        columns.put("engine","Text");
        columns.put("make","Text");
        columns.put("model","Text");
        columns.put("year","Text");
        columns.put("tank_size","Text");
        columns.put("totalMileage","Integer");
        columns.put("trimLevel","Integer");
        columns.put("pendingEdmundServices","Text");
        columns.put("pendingIntervalServices","Text");
        columns.put("pendingFixedServices","Text");
        columns.put("dtcs","Text");
        columns.put("recalls","Text");
        columns.put("dealership","Text");
    }

}

package com.pitstop.DataAccessLayer.Models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/11/2016.
 */
public class Car implements Serializable {
    private long id;
    private long userId;
    private String vin;
    private String year;
    private String make;
    private String model;
    private String trim;
    private String engine;
    private String tankSize;
    private String cityMileage;
    private String highwayMileage;
    private long baseMileage;
    private long totalMileage;
    private List<String> scanner;
    private List<String> issues;
    @SerializedName("shop")
    private Dealership dealership;

    public Car(){}

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getVin() {
        return vin;
    }

    public String getYear() {
        return year;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getTrim() {
        return trim;
    }

    public String getEngine() {
        return engine;
    }

    public String getTankSize() {
        return tankSize;
    }

    public String getCityMileage() {
        return cityMileage;
    }

    public String getHighwayMileage() {
        return highwayMileage;
    }

    public long getBaseMileage() {
        return baseMileage;
    }

    public long getTotalMileage() {
        return totalMileage;
    }

    public List<String> getScanner() {
        return scanner;
    }

    public List<String> getIssues() {
        return issues;
    }

    public Dealership getDealership() {
        return dealership;
    }
}

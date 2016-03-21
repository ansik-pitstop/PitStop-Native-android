package com.pitstop.DataAccessLayer.DTOs;

import android.util.Log;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Paul Soladoye on 2/11/2016.
 */
public class Car implements Serializable {
    @SerializedName("id")
    private String cardId;

    private String make;
    private String model;
    private int year;
    private String trim;
    private String vin;
    private String engine;
    private String tankSize;
    private String cityMileage;
    private String highwayMileage;
    private long baseMileage;
    private long totalMileage;

    private String ownerId;

    @SerializedName("shop")
    private Dealership dealerShip;
    private boolean serviceDue;

    private List<String> scanner;
    private List<CarIssue> issues;

    public Car() { }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getTrim() {
        return trim;
    }

    public void setTrim(String trim) {
        this.trim = trim;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getTankSize() {
        return tankSize;
    }

    public void setTankSize(String tankSize) {
        this.tankSize = tankSize;
    }

    public String getCityMileage() {
        return cityMileage;
    }

    public void setCityMileage(String cityMileage) {
        this.cityMileage = cityMileage;
    }

    public String getHighwayMileage() {
        return highwayMileage;
    }

    public void setHighwayMileage(String highwayMileage) {
        this.highwayMileage = highwayMileage;
    }

    public long getBaseMileage() {
        return baseMileage;
    }

    public void setBaseMileage(long baseMileage) {
        this.baseMileage = baseMileage;
    }

    public long getTotalMileage() {
        return totalMileage;
    }

    public void setTotalMileage(long totalMileage) {
        this.totalMileage = totalMileage;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Dealership getDealerShip() {
        return dealerShip;
    }

    public void setDealerShip(Dealership dealerShip) {
        this.dealerShip = dealerShip;
    }

    public boolean isServiceDue() {
        return serviceDue;
    }

    public void setServiceDue(boolean serviceDue) {
        this.serviceDue = serviceDue;
    }

    public List<CarIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<CarIssue> issues) {
        this.issues = issues;
    }

    public static Car jsonToCarObject(JSONObject jsonObject) {
        Car car  = new Car();
        String json;
        try {
            json = jsonObject.getString("data");
            car = JsonUtil.json2object(json,Car.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return car;
    }

    public String objectToJson() {
        String json = null;

        try {
            json = JsonUtil.object2Json(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  json;
    }
}

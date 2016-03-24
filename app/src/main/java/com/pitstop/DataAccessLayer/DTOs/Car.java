package com.pitstop.DataAccessLayer.DTOs;

import android.util.Log;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
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
    private double baseMileage;
    private double totalMileage;
    private int numberOfRecalls;
    private int numberOfServices;

    private String ownerId;
    private String shopId; // TODO remove once api integration is complete

    @SerializedName("shop")
    private Dealership dealerShip;
    private boolean serviceDue;

    private String scanner;
    private List<CarIssue> issues = new ArrayList<>();

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

    public double getBaseMileage() {
        return baseMileage;
    }

    public void setBaseMileage(double baseMileage) {
        this.baseMileage = baseMileage;
    }

    public double getTotalMileage() {
        return totalMileage;
    }

    public void setTotalMileage(double totalMileage) {
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

    public int getNumberOfRecalls() {
        return numberOfRecalls;
    }

    public void setNumberOfRecalls(int numberOfRecalls) {
        this.numberOfRecalls = numberOfRecalls;
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    public void setNumberOfServices(int numberOfServices) {
        this.numberOfServices = numberOfServices;
    }

    public String getScanner() {
        return scanner;
    }

    public void setScanner(String scanner) {
        this.scanner = scanner;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
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

    // TODO update param once api integration is complete
    public static Car createCar(ParseObject parseObject) {
        Car car = null;
        if(parseObject != null) {
            car = new Car();
            car.setCardId(parseObject.getObjectId());
            car.setEngine(parseObject.getString("engine"));
            car.setMake(parseObject.getString("make"));
            car.setModel(parseObject.getString("model"));
            car.setYear(parseObject.getInt("year"));
            car.setNumberOfRecalls(parseObject.getInt("numberOfRecalls"));
            car.setNumberOfServices(parseObject.getInt("numberOfServices"));
            car.setScanner(parseObject.getString("scannerId"));
            car.setTotalMileage(parseObject.getDouble("totalMileage"));
            car.setBaseMileage(parseObject.getDouble("baseMileage"));
            car.setOwnerId(parseObject.getString("owner"));
            car.setShopId(parseObject.getString("dealership"));
            car.setVin(parseObject.getString("VIN"));
            car.setServiceDue(parseObject.getBoolean("serviceDue"));
        }
        return car;
    }
}
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
    private int baseMileage;
    private int totalMileage;
    private int numberOfRecalls;
    private int numberOfServices;
    private boolean currentCar;

    private String ownerId;
    // TODO remove once api integration is complete
    private String shopId;
    private List<String> pendingEdmundServicesIds = new ArrayList<>();
    private List<String> pendingIntervalServicesIds = new ArrayList<>();
    private List<String> pendingFixedServicesIds = new ArrayList<>();
    private List<String> storedDTCs = new ArrayList<>();

    @SerializedName("shop")
    private Dealership dealerShip;
    private boolean serviceDue;

    private String scanner;
    private List<CarIssue> issues = new ArrayList<>();

    private ParseObject parseObject;

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

    public int getBaseMileage() {
        return baseMileage;
    }

    public void setBaseMileage(int baseMileage) {
        this.baseMileage = baseMileage;
    }

    public int getTotalMileage() {
        return totalMileage;
    }

    public void setTotalMileage(int totalMileage) {
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

    public boolean isCurrentCar() {
        return currentCar;
    }

    public void setCurrentCar(boolean currentCar) {
        this.currentCar = currentCar;
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

    //Todo remove once api-integration is done
    public List<String> getPendingEdmundServicesIds() {
        return pendingEdmundServicesIds;
    }

    public void setPendingEdmundServicesIds(List<String> pendingEdmundServicesIds) {
        this.pendingEdmundServicesIds = pendingEdmundServicesIds;
    }

    public List<String> getPendingIntervalServicesIds() {
        return pendingIntervalServicesIds;
    }

    public void setPendingIntervalServicesIds(List<String> pendingIntervalServicesIds) {
        this.pendingIntervalServicesIds = pendingIntervalServicesIds;
    }

    public List<String> getPendingFixedServicesIds() {
        return pendingFixedServicesIds;
    }

    public void setPendingFixedServicesIds(List<String> pendingFixedServicesIds) {
        this.pendingFixedServicesIds = pendingFixedServicesIds;
    }

    public List<String> getStoredDTCs() {
        return storedDTCs;
    }

    public void setStoredDTCs(List<String> storedDTCs) {
        this.storedDTCs = storedDTCs;
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
            Log.i("MAIN_ACTIVITY-->", "ParseId: " + car.getCardId());
            car.setEngine(parseObject.getString("engine"));
            car.setMake(parseObject.getString("make"));
            car.setModel(parseObject.getString("model"));
            car.setYear(parseObject.getInt("year"));
            car.setNumberOfRecalls(parseObject.getInt("numberOfRecalls"));
            car.setNumberOfServices(parseObject.getInt("numberOfServices"));
            car.setScanner(parseObject.getString("scannerId"));
            car.setTotalMileage(parseObject.getInt("totalMileage"));
            car.setBaseMileage(parseObject.getInt("baseMileage"));
            car.setOwnerId(parseObject.getString("owner"));
            car.setShopId(parseObject.getString("dealership"));
            car.setVin(parseObject.getString("VIN"));
            car.setServiceDue(parseObject.getBoolean("serviceDue"));
            car.setCurrentCar(parseObject.getBoolean("currentCar"));
            car.setPendingEdmundServicesIds(parseObject.<String>getList("pendingEdmundServices"));
            car.setPendingFixedServicesIds(parseObject.<String>getList("pendingFixedServices"));
            car.setPendingIntervalServicesIds(parseObject.<String>getList("pendingIntervalServices"));
            car.setStoredDTCs(parseObject.<String>getList("storedDTCs"));
        }
        return car;
    }

    public static List<Car> createCarsList(List<ParseObject> objects) {
        List<Car> cars = new ArrayList<>();
        for(ParseObject object : objects) {
            cars.add(createCar(object));
        }
        return cars;
    }
}
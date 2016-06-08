package com.pitstop.DataAccessLayer.DTOs;

import com.castel.obd.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 2/11/2016.
 */
public class Car implements Serializable {

    private int id;

    private String parseId;

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
    private int numberOfRecalls = 0;
    private int numberOfServices;
    private boolean currentCar;

    private String ownerId;
    private int userId;
    private int shopId;

    private Dealership dealership;
    private boolean serviceDue;

    private String scannerId;
    private List<CarIssue> issues = new ArrayList<>();

    public Car() { }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getParseId() {
        return parseId;
    }

    public void setParseId(String parseId) {
        this.parseId = parseId;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Dealership getDealership() {
        return dealership;
    }

    public void setDealership(Dealership dealership) {
        this.dealership = dealership;
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

    public String getScannerId() {
        return scannerId;
    }

    public void setScannerId(String scannerId) {
        this.scannerId = scannerId;
    }

    public boolean isCurrentCar() {
        return currentCar;
    }

    public void setCurrentCar(boolean currentCar) {
        this.currentCar = currentCar;
    }

    public int getShopId() {
        return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    public List<CarIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<CarIssue> issues) {
        this.issues = issues;
    }

    public ArrayList<CarIssue> getActiveIssues() {
        ArrayList<CarIssue> activeIssues = new ArrayList<>();

        for(CarIssue issue : issues) {
            if(!issue.getStatus().equals(CarIssue.ISSUE_DONE)) {
                activeIssues.add(issue);
            }
        }

        return activeIssues;
    }

    public ArrayList<CarIssue> getDoneIssues() {
        ArrayList<CarIssue> activeIssues = new ArrayList<>();

        for(CarIssue issue : issues) {
            if(issue.getStatus().equals(CarIssue.ISSUE_DONE)) {
                activeIssues.add(issue);
            }
        }

        return activeIssues;
    }

    // create car from json object (this is for the response from POST car)
    public static Car createNewCar(JSONObject jsonObject) throws JSONException {
        Car car = new Car();

        car.setId(jsonObject.getInt("id"));
        car.setEngine(jsonObject.getString("car_engine"));
        car.setMake(jsonObject.getString("car_make"));
        car.setModel(jsonObject.getString("car_model"));
        car.setYear(jsonObject.getInt("car_year"));
        car.setTrim(jsonObject.getString("car_trim"));
        car.setScannerId(jsonObject.optString("scannerId"));
        car.setTotalMileage(jsonObject.getInt("mileage_total"));
        car.setBaseMileage(jsonObject.getInt("mileage_base"));
        car.setUserId(jsonObject.getInt("id_user"));
        car.setShopId(jsonObject.getJSONObject("shop").getInt("id_shop"));
        car.setVin(jsonObject.getString("vin"));

        //car.setIssues(CarIssue.createCarIssues(jsonObject.getJSONArray("issues"), car.getId()));

        return car;
    }

    // create car from json response (this is for GET from api)
    public static Car createCar(String response) throws JSONException {
        Car car = JsonUtil.json2object(response, Car.class);

        JSONObject jsonObject = new JSONObject(response);

        if(!jsonObject.isNull("issues")) {
            car.setIssues(CarIssue.createCarIssues(jsonObject.getJSONArray("issues"), car.getId()));
            car.setNumberOfServices(jsonObject.getJSONArray("issues").length());
        }

        if(!jsonObject.isNull("shop")) {
            Dealership dealer = Dealership.jsonToDealershipObject(jsonObject.getJSONObject("shop").toString());
            car.setDealership(dealer);
            car.setShopId(dealer.getId());
        }

        if(!jsonObject.isNull("scanner")) {
            car.setScannerId(jsonObject.getJSONObject("scanner").getString("scannerId"));
        }

        return car;
    }

    // create car from json object (this is for GET from api)
    public static Car createCar(JSONObject jsonObject) throws JSONException { // THIS IS OUTDATED
        Car car = new Car();

        car.setId(jsonObject.getInt("id"));
        car.setEngine(jsonObject.getString("engine"));
        car.setMake(jsonObject.getString("make"));
        car.setModel(jsonObject.getString("model"));
        car.setYear(jsonObject.getInt("year"));
        car.setTrim(jsonObject.getString("trim"));
        car.setScannerId(jsonObject.optString("scannerId"));
        car.setTotalMileage(jsonObject.getInt("totalMileage"));
        car.setBaseMileage(jsonObject.getInt("baseMileage"));
        car.setUserId(jsonObject.getInt("userId"));
        car.setShopId(jsonObject.getJSONObject("shop").getInt("id"));
        car.setVin(jsonObject.getString("vin"));

        Object issues = jsonObject.get("issues");

        if(issues instanceof JSONArray) {
            car.setIssues(CarIssue.createCarIssues(jsonObject.getJSONArray("issues"), car.getId()));
            car.setNumberOfServices(((JSONArray) issues).length());
        }

        if(jsonObject.get("shop") != null) {
            car.setDealership(Dealership.jsonToDealershipObject(jsonObject.getJSONObject("shop").toString()));
        }

        return car;
    }

    // create list of cars from api response
    public static List<Car> createCarsList(String jsonRoot) throws JSONException {
        List<Car> cars = new ArrayList<>();
        JSONArray carArr;

        try {
            carArr = new JSONArray(jsonRoot);
        } catch (JSONException e) {
            return cars;
        }

        for(int i = 0 ; i < carArr.length() ; i++) {
            cars.add(createCar(carArr.getString(i)));
        }

        return cars;
    }
}
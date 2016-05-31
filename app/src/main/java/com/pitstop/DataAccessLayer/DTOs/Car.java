package com.pitstop.DataAccessLayer.DTOs;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

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

    private String scanner;
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

    /*public static Car createCar(ParseObject parseObject) {
        Car car = null;
        if(parseObject != null) {
            car = new Car();
            car.setParseId(parseObject.getObjectId());
            car.setEngine(parseObject.getString("engine"));
            car.setMake(parseObject.getString("make"));
            car.setModel(parseObject.getString("model"));
            car.setYear(parseObject.getInt("year"));
            car.setTrim(parseObject.getString("trim_level"));
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
            car.setPendingDTCs(parseObject.<String>getList("pendingDTCs"));
        }
        return car;
    }*/

    // create car from json object (this is for the response from POST car)
    public static Car createNewCar(JSONObject jsonObject) throws JSONException {
        Car car = new Car();

        car.setId(jsonObject.getInt("id"));
        car.setEngine(jsonObject.getString("car_engine"));
        car.setMake(jsonObject.getString("car_make"));
        car.setModel(jsonObject.getString("car_model"));
        car.setYear(jsonObject.getInt("car_year"));
        car.setTrim(jsonObject.getString("car_trim"));
        car.setScanner(jsonObject.optString("scannerId"));
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

    // create car from json object (this is for GET from api)
    public static Car createCar(JSONObject jsonObject) throws JSONException {
        Car car = new Car();

        car.setId(jsonObject.getInt("id"));
        car.setEngine(jsonObject.getString("engine"));
        car.setMake(jsonObject.getString("make"));
        car.setModel(jsonObject.getString("model"));
        car.setYear(jsonObject.getInt("year"));
        car.setTrim(jsonObject.getString("trim"));
        car.setScanner(jsonObject.optString("scannerId"));
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

   /* public static List<Car> createCarsList(List<ParseObject> objects) {
        List<Car> cars = new ArrayList<>();
        for(ParseObject object : objects) {
            cars.add(createCar(object));
        }
        return cars;
    }*/

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
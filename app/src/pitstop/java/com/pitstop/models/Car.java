package com.pitstop.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.castel.obd.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 2/11/2016.
 */
public class Car implements Parcelable {

    public class Make{
        public static final String RAM = "ram";
        public static final String CHRYSLER = "chrysler";
        public static final String DODGE = "dodge";
        public static final String JEEP = "jeep";
    }
    /**
     * Car id
     */
    private int id;
    private String make;
    private String model;
    private int year;
    private String trim;
    private String vin;
    private String engine;
    private String tankSize;
    private String cityMileage;
    private String highwayMileage;

    /**
     * Mileage entered by the user when they added their car
     */
    private double baseMileage;
    /**
     * Current mileage either on what user manually entered and the mileage from user trips
     */
    private double totalMileage;
    /**
     * <p>The mileage that's shown for live mileage updates in the car scan activity</p>
     * <p>gets updated locally every 2 seconds but that's sent to the backend</p>
     */
    private double displayedMileage; // for live mileage updates

    private int numberOfRecalls = 0;
    private int numberOfServices;
    private boolean currentCar;

    private int userId;
    private int shopId;
    private Dealership shop;

    private boolean serviceDue;

    private String scannerId;

    private CarScanner scanner;

    public Car() { }

    public Dealership getShop() {
        return shop;
    }

    public void setShop(Dealership shop) {
        this.shopId = shop.getId();
        this.shop = shop;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setBaseMileage(int baseMileage) {
        this.baseMileage = baseMileage;
    }

    public double getTotalMileage() {
        return totalMileage;
    }

    public void setTotalMileage(double totalMileage) {
        this.totalMileage = totalMileage;
    }

    public double getDisplayedMileage() {
        return displayedMileage;
    }

    public void setDisplayedMileage(double displayedMileage) {
        this.displayedMileage = displayedMileage;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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
        if (getScanner() != null)
            return getScanner().getScannerId();
        else return scannerId;
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

    public CarScanner getScanner() {
        return scanner;
    }

    public void setScanner(CarScanner scanner) {
        this.scanner = scanner;
    }

    public int getShopId() {
        if (shop != null) return shop.getId();
        else return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    // create car from json response (this is for GET from api)
    public static Car createCar(String response) throws JSONException {
        Car car = JsonUtil.json2object(response, Car.class);

        car.setDisplayedMileage(car.getTotalMileage());

        JSONObject jsonObject = new JSONObject(response);

        if(!jsonObject.isNull("shop")) {
            Dealership dealer = Dealership.jsonToDealershipObject(jsonObject.getJSONObject("shop").toString());
            car.setShopId(dealer.getId());
        }

        if(!jsonObject.isNull("scannerId")) {
            car.setScannerId(jsonObject.getJSONObject("scannerId").getString("scannerId"));
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

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", vin='" + vin + '\'' +
                ", totalMileage=" + totalMileage +
                ", baseMileage=" + baseMileage +
                ", scannerId='" + scannerId + '\'' +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", dealership= "+getShop()+
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.make);
        dest.writeString(this.model);
        dest.writeInt(this.year);
        dest.writeString(this.trim);
        dest.writeString(this.vin);
        dest.writeString(this.engine);
        dest.writeString(this.tankSize);
        dest.writeString(this.cityMileage);
        dest.writeString(this.highwayMileage);
        dest.writeDouble(this.baseMileage);
        dest.writeDouble(this.totalMileage);
        dest.writeDouble(this.displayedMileage);
        dest.writeInt(this.numberOfRecalls);
        dest.writeInt(this.numberOfServices);
        dest.writeByte(this.currentCar ? (byte) 1 : (byte) 0);
        dest.writeInt(this.userId);
        dest.writeInt(this.shopId);
        dest.writeByte(this.serviceDue ? (byte) 1 : (byte) 0);
        dest.writeString(this.scannerId);
    }

    protected Car(Parcel in) {
        this.id = in.readInt();
        this.make = in.readString();
        this.model = in.readString();
        this.year = in.readInt();
        this.trim = in.readString();
        this.vin = in.readString();
        this.engine = in.readString();
        this.tankSize = in.readString();
        this.cityMileage = in.readString();
        this.highwayMileage = in.readString();
        this.baseMileage = in.readDouble();
        this.totalMileage = in.readDouble();
        this.displayedMileage = in.readDouble();
        this.numberOfRecalls = in.readInt();
        this.numberOfServices = in.readInt();
        this.currentCar = in.readByte() != 0;
        this.userId = in.readInt();
        this.shopId = in.readInt();
        this.serviceDue = in.readByte() != 0;
        this.scannerId = in.readString();
    }

    public static final Parcelable.Creator<Car> CREATOR = new Parcelable.Creator<Car>() {
        @Override
        public Car createFromParcel(Parcel source) {
            return new Car(source);
        }

        @Override
        public Car[] newArray(int size) {
            return new Car[size];
        }
    };
}
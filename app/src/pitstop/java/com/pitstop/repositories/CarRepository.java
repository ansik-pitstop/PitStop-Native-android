package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

public class CarRepository implements Repository{

    private static CarRepository INSTANCE;
    private LocalCarAdapter localCarAdapter;
    private NetworkHelper networkHelper;

    public static synchronized CarRepository getInstance(LocalCarAdapter localCarAdapter
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new CarRepository(localCarAdapter, networkHelper);
        }
        return INSTANCE;
    }

    public CarRepository(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper){
        this.localCarAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    public void insert(Car car, boolean hasShop, Callback<Car> callback) {
        //Insert to backend
        JSONObject body = new JSONObject();

        try {
            body.put("vin", car.getVin());
            body.put("baseMileage", car.getTotalMileage());
            body.put("userId", car.getUserId());
            body.put("scannerId", car.getScannerId());
            if (hasShop){
                body.put("shopId", car.getShopId());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.post("car", getInsertCarRequestCallback(callback), body);

    }

    private RequestCallback getInsertCarRequestCallback(Callback<Car> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        Car car = null;
                        try{
                            car = Car.createCar(response);
                        }
                        catch (JSONException e){
                            e.printStackTrace();
                            callback.onError(RequestError.getUnknownError());
                        }

                        localCarAdapter.deleteCar(car.getId());
                        localCarAdapter.storeCarData(car);
                        callback.onSuccess(car);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    callback.onError(RequestError.getUnknownError());
                    return;
                }
            }
        };

        return requestCallback;
    }

    public void update(Car car, Callback<Object> callback) {

        //Update backend
        networkHelper.updateCar(car.getId(),car.getTotalMileage()
                ,car.getShopId(),getUpdateCarRequestCallback(callback,car));
    }

    private RequestCallback getUpdateCarRequestCallback(Callback<Object> callback, Car car){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    localCarAdapter.updateCar(car);
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void getCarsByUserId(int userId, Callback<List<Car>> callback ){

        networkHelper.getCarsByUserId(userId,getCarsRequestCallback(callback, userId));
    }

    private RequestCallback getCarsRequestCallback(Callback<List<Car>> callback, int userId){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {

                    if (requestError == null && response != null){
                        List<Car> cars = new ArrayList<>();

                        JSONArray carsJson = new JSONArray();
                        try{
                            carsJson  = new JSONArray(response);
                        }catch (JSONException e){

                        }

                        for(int i = 0;i<carsJson.length();i++){
                            try{
                                cars.add(Car.createCar(carsJson.getString(i)));
                            }catch (Exception e){

                            }
                        }
                        networkHelper.getUserSettingsById(userId, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(response != null && requestError == null){
                                    try{
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONObject userJson = responseJson.getJSONObject("user");
                                        int mainCarId;
                                        if(userJson.has("mainCar")){
                                            mainCarId = userJson.getInt("mainCar");
                                        } else{
                                            mainCarId = 0;
                                        }
                                        JSONArray customShops;
                                        if(userJson.has("customShops")) {
                                            customShops = userJson.getJSONArray("customShops");
                                            for (Car c : cars) {
                                                if (c.getId() == mainCarId) {
                                                    c.setCurrentCar(true);
                                                }
                                                if (c.getDealership() != null) {//stops crash for cars with no shop
                                                    for (int i = 0; i < customShops.length(); i++) {
                                                        JSONObject shop = customShops.getJSONObject(i);
                                                        if (c.getDealership().getId() == shop.getInt("id")) {
                                                            Dealership d = Dealership.jsonToDealershipObject(shop.toString());
                                                            d.setCustom(true);
                                                            c.setDealership(d);
                                                        }
                                                    }
                                                } else {
                                                    Dealership noDealer = new Dealership();
                                                    noDealer.setName("No Dealership");
                                                    noDealer.setId(19);
                                                    noDealer.setEmail("info@getpitstop.io");
                                                    noDealer.setCustom(true);
                                                    c.setDealership(noDealer);
                                                }
                                            }
                                        }else{
                                            for (Car c : cars) {
                                                if (c.getId() == mainCarId) {
                                                    c.setCurrentCar(true);
                                                }
                                                if(c.getDealership() == null){
                                                    Dealership noDealer = new Dealership();
                                                    noDealer.setName("No Dealership");
                                                    noDealer.setId(19);
                                                    noDealer.setEmail("info@getpitstop.io");
                                                    noDealer.setCustom(true);
                                                    c.setDealership(noDealer);
                                                }
                                            }
                                        }
                                        localCarAdapter.deleteAllCars();
                                        localCarAdapter.storeCars(cars);
                                        callback.onSuccess(cars);
                                    }catch (JSONException e){
                                        callback.onError(RequestError.getUnknownError());
                                    }
                                }else{
                                    callback.onError(requestError);
                                }
                            }
                        });
                    }
                    else{
                        callback.onError(requestError);
                    }
            }
        };

        return requestCallback;
    }

    public void get(int id,int userId, Callback<Car> callback) {
        networkHelper.getCarsById(id,getGetCarRequestCallback(callback,userId));
    }

    private RequestCallback getGetCarRequestCallback(Callback<Car> callback, int userId){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        Car car = Car.createCar(response);
                        networkHelper.getUserSettingsById(userId, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(response != null){
                                    try{
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONArray customShops;
                                        if(responseJson.getJSONObject("user").has("customShops")) {
                                            customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                                            for (int i = 0; i < customShops.length(); i++) {
                                                JSONObject shop = customShops.getJSONObject(i);
                                                if (car.getDealership() != null) {
                                                    if (car.getDealership().getId() == shop.getInt("id")) {
                                                        Dealership dealership = Dealership.jsonToDealershipObject(shop.toString());
                                                        dealership.setCustom(true);
                                                        car.setDealership(dealership);
                                                    }
                                                } else {
                                                    Dealership noDealer = new Dealership();
                                                    noDealer.setName("No Dealership");
                                                    noDealer.setId(19);
                                                    noDealer.setEmail("info@getpitstop.io");
                                                    noDealer.setCustom(true);
                                                    car.setDealership(noDealer);
                                                }
                                            }
                                        }else{
                                            if(car.getDealership() == null){
                                                Dealership noDealer = new Dealership();
                                                noDealer.setName("No Dealership");
                                                noDealer.setId(19);
                                                noDealer.setEmail("info@getpitstop.io");
                                                noDealer.setCustom(true);
                                                car.setDealership(noDealer);
                                            }
                                        }
                                        localCarAdapter.deleteCar(car.getId());
                                        localCarAdapter.storeCarData(car);
                                        callback.onSuccess(car);
                                    }catch (JSONException e){
                                        callback.onError(RequestError.getUnknownError());
                                        e.printStackTrace();
                                    }
                                }else{
                                    callback.onError(requestError);
                                }
                            }
                        });


                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JSONException e){
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };
        return requestCallback;
    }

    public void delete(int carId, Callback<Object> callback) {
        networkHelper.deleteUserCar(carId,getDeleteCarRequestCallback(callback, carId));
    }

    private RequestCallback getDeleteCarRequestCallback(Callback<Object> callback, int carId){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        localCarAdapter.deleteCar(carId);
                        callback.onSuccess(response);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };

        return requestCallback;
    }
}

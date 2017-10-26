package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.JsonIOException;
import com.pitstop.BuildConfig;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.models.Car;
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

    private final String TAG = getClass().getSimpleName();

    private static CarRepository INSTANCE;
    private LocalCarStorage localCarStorage;
    private NetworkHelper networkHelper;

    public static synchronized CarRepository getInstance(LocalCarStorage localCarStorage
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new CarRepository(localCarStorage, networkHelper);
        }
        return INSTANCE;
    }

    public CarRepository(LocalCarStorage localCarStorage, NetworkHelper networkHelper){
        this.localCarStorage = localCarStorage;
        this.networkHelper = networkHelper;
    }

    public void getCarByVin(String vin, Callback<Car> callback){
        networkHelper.get("car/?vin=" + vin, getGetCarByVinRequestCallback(callback));
    }

    private RequestCallback getGetCarByVinRequestCallback(Callback<Car> callback){
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){

                    //No car found
                    if (response == null || response.equals("{}")){
                        callback.onSuccess(null);
                        return;
                    }
                    //Create car
                    try{
                        Car car = Car.createCar(response);
                        callback.onSuccess(car);

                    }catch(JSONException e){
                        e.printStackTrace();
                        callback.onError(RequestError.getUnknownError());
                    }
                }
                else{
                    callback.onError(requestError);
                }
            }
        };
    }

    public void getShopId(int carId, Callback<Integer> callback){
        networkHelper.get("v1/car/shop?carId=" + carId, (response, requestError) -> {
            if (requestError == null){
                Log.d(TAG,"getShopId resposne: "+response);
                try{
                    JSONObject jsonResponse = new JSONObject(response)
                            .getJSONArray("response").getJSONObject(0);
                    callback.onSuccess(jsonResponse.getInt("shopId"));
                }catch(JSONException e){
                    e.printStackTrace();
                    callback.onError(RequestError.getUnknownError());
                }
            }
        });
    }

    public void insert(String vin, double baseMileage, int userId, String scannerId
            , Callback<Car> callback) {
        //Insert to backend
        JSONObject body = new JSONObject();

        try {
            body.put("vin", vin);
            body.put("baseMileage", baseMileage);
            body.put("userId", userId);
            body.put("scannerId", scannerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.post("car", getInsertCarRequestCallback(callback), body);

    }

    private RequestCallback getInsertCarRequestCallback(Callback<Car> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
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

                    localCarStorage.deleteCar(car.getId());
                    localCarStorage.storeCarData(car);
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
        };

        return requestCallback;
    }

    public void update(Car car, Callback<Object> callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("carId", car.getId());
            body.put("totalMileage", car.getTotalMileage());
            body.put("shopId", car.getShopId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.put("car", getUpdateCarRequestCallback(callback,car), body);
    }

    private RequestCallback getUpdateCarRequestCallback(Callback<Object> callback, Car car){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            if (requestError == null){
                localCarStorage.updateCar(car);
                callback.onSuccess(response);
            }
            else{
                callback.onError(requestError);
            }
        };

        return requestCallback;
    }

    public void getCarsByUserId(int userId, Callback<List<Car>> callback ){

        networkHelper.getCarsByUserId(userId,getCarsRequestCallback(callback, userId));
    }

    private RequestCallback getCarsRequestCallback(Callback<List<Car>> callback, int userId){
        RequestCallback requestCallback = (response, requestError) -> {

                if (requestError == null && response != null){
                    List<Car> cars = new ArrayList<>();

                    //Return empty list if no cars returned
                    if (response.equals("{}")){
                        callback.onSuccess(cars);
                        return;
                    }

                    JSONArray carsJson = new JSONArray();
                    try{
                        carsJson  = new JSONArray(response);
                    }catch (JSONException e){

                    }

                    for(int i = 0;i<carsJson.length();i++){
                        try{
                            //Todo: remove correcting shop id below
                            cars.add(Car.createCar(carsJson.getString(i)));
                            if (cars.get(i).getShopId() == 0)
                                if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)
                                        || BuildConfig.DEBUG)
                                    cars.get(i).setShopId(1);
                                else
                                    cars.get(i).setShopId(19);
                        }catch (Exception e){

                        }
                    }
                    localCarStorage.deleteAllCars();
                    localCarStorage.storeCars(cars);
                    callback.onSuccess(cars);
                }
                else{
                    callback.onError(requestError);
                }
        };

        return requestCallback;
    }

    public void get(int id,int userId, Callback<Car> callback) {
        networkHelper.getCarsById(id,getGetCarRequestCallback(callback));
    }

    private RequestCallback getGetCarRequestCallback(Callback<Car> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            try {
                if (requestError == null){
                    Car car = Car.createCar(response);
                    if (car.getShopId() == 0)
                        //Todo: remove correcting shopId below
                        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)
                                || BuildConfig.DEBUG)
                            car.setShopId(1);
                        else
                            car.setShopId(19);

                    localCarStorage.deleteCar(car.getId());
                    localCarStorage.storeCarData(car);
                    callback.onSuccess(car);
                }
                else{
                    callback.onError(requestError);
                }
            }
            catch(JSONException e){
                callback.onError(RequestError.getUnknownError());
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
                        localCarStorage.deleteCar(carId);
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

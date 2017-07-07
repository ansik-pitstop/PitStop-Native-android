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

import java.util.List;

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

public class CarRepository {

    private static CarRepository INSTANCE;
    private LocalCarAdapter localCarAdapter;
    private NetworkHelper networkHelper;

    public interface CarInsertCallback{
        void onCarAdded();
        void onError();
    }

    public interface CarUpdateCallback{
        void onCarUpdated();
        void onError();
    }

    public interface CarGetCallback{
        void onCarGot(Car car);
        void onError();
    }
    public interface CarsGetCallback{
        void onCarsGot(List<Car> cars);
        void onNoCarsGot(List<Car> cars);
        void onError();
    }

    public interface CarDeleteCallback{
        void onCarDeleted();
        void onError();
    }

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

    public boolean insert(Car model, CarInsertCallback callback) {

        //Insert locally
        if (localCarAdapter.getCar(model.getId()) == null){
            localCarAdapter.storeCarData(model);
        }
        else{
            return false; //Car already inserted
        }

        //Insert to backend
        networkHelper.createNewCar(model.getUserId(),(int)model.getTotalMileage()
            ,model.getVin(),model.getScannerId(),model.getShopId()
                ,getInsertCarRequestCallback(callback));

        return true;
    }

    private RequestCallback getInsertCarRequestCallback(CarInsertCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarAdded();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public boolean update(Car model, CarUpdateCallback callback) {

        //No rows updated, therefore updating car that doesnt exist
        if (localCarAdapter.updateCar(model) == 0){
            return false;
        }
        //Update backend
        networkHelper.updateCar(model.getId(),model.getTotalMileage()
                ,model.getShopId(),getUpdateCarRequestCallback(callback));

        return true;
    }

    private RequestCallback getUpdateCarRequestCallback(CarUpdateCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarUpdated();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public List<Car> getCarsByUserId(int userId, CarsGetCallback callback ){
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        networkHelper.getCarsByUserId(userId,getCarsRequestCallback(callback, userId));
        return localCarAdapter.getCarsByUserId(userId);
    }
    private RequestCallback getCarsRequestCallback(CarsGetCallback callback, int userId){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null && response != null){
                        List<Car> cars = Car.createCarsList(response);
                        networkHelper.getUserSettingsById(userId, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                System.out.println("Testing "+response);
                                if(response != null && requestError == null){
                                    try{
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONObject userJson = responseJson.getJSONObject("user");
                                        int mainCarId = userJson.getInt("mainCar");
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
                                        callback.onCarsGot(cars);
                                    }catch (JSONException e){
                                        callback.onError();
                                    }
                                }else{
                                    callback.onError();
                                }
                            }
                        });
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){
                    callback.onError();
                }
            }
        };

        return requestCallback;
    }

    public Car get(int id,int userId, CarGetCallback callback) {
        networkHelper.getCarsById(id,getGetCarRequestCallback(callback,userId));
        return localCarAdapter.getCar(id);
    }

    private RequestCallback getGetCarRequestCallback(CarGetCallback callback, int userId){
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
                                        callback.onCarGot(car);
                                    }catch (JSONException e){
                                        callback.onError();
                                        e.printStackTrace();
                                    }
                                }else{
                                    callback.onError();
                                }
                            }
                        });


                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){

                }
            }
        };

        return requestCallback;
    }

    public boolean delete(int carId, CarDeleteCallback callback) {
        //Check if car exists before deleting
        if (localCarAdapter.getCar(carId) == null)
            return false;

        localCarAdapter.deleteCar(carId);
        networkHelper.deleteUserCar(carId,getDeleteCarRequestCallback(callback));

        return true;
    }

    private RequestCallback getDeleteCarRequestCallback(CarDeleteCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarDeleted();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }
}

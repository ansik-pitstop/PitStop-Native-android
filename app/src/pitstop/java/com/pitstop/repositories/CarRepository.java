package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;

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

    interface CarInsertCallback{
        void onCarAdded();
        void onError();
    }

    interface CarUpdateCallback{
        void onCarUpdated();
        void onError();
    }

    interface CarGetCallback{
        void onCarGot(Car car);
        void onError();
    }

    interface CarDeleteCallback{
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

    public Car get(int id, RequestCallback callback) {
        networkHelper.getCarsById(id,callback);
        return localCarAdapter.getCar(id);
    }

    private RequestCallback getGetCarRequestCallback(CarGetCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarGot(Car.createCar(response));
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

    public boolean delete(Car model, CarDeleteCallback callback) {

        //Check if car exists before deleting
        if (localCarAdapter.getCar(model.getId()) == null)
            return false;

        localCarAdapter.deleteCar(model);
        networkHelper.deleteUserCar(model.getId(),getDeleteCarRequestCallback(callback));

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

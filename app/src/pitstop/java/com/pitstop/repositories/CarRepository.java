package com.pitstop.repositories;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;
import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

public class CarRepository {

    private LocalCarAdapter localCarAdapter;
    private NetworkHelper networkHelper;

    public CarRepository(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper){
        this.localCarAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    public boolean insert(Car model, RequestCallback callback) {

        //Insert locally
        if (localCarAdapter.getCar(model.getId()) == null){
            localCarAdapter.storeCarData(model);
        }
        else{
            return false; //Car already inserted
        }

        //Insert to backend
        networkHelper.createNewCar(model.getUserId(),(int)model.getTotalMileage()
            ,model.getVin(),model.getScannerId(),model.getShopId(),callback);

        return true;
    }

    public boolean update(Car model, RequestCallback callback) {

        //No rows updated, therefore updating car that doesnt exist
        if (localCarAdapter.updateCar(model) == 0){
            return false;
        }

        //Update backend
        networkHelper.updateCarMileage(model.getId(),model.getTotalMileage(),callback);
        networkHelper.updateCarShop(model.getId(),model.getShopId(),callback);

        return true;
    }

    public Car get(int id, RequestCallback callback) {
        networkHelper.getCarsById(id,callback);
        return localCarAdapter.getCar(id);
    }

    public boolean delete(Car model, RequestCallback callback) {

        //Check if car exists before deleting
        if (localCarAdapter.getCar(model.getId()) == null)
            return false;

        localCarAdapter.deleteCar(model);
        networkHelper.deleteUserCar(model.getId(),callback);

        return true;
    }
}

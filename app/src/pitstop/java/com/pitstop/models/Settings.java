package com.pitstop.models;

import android.util.Log;

import java.util.concurrent.Semaphore;

/**
 * Not complete, doesn't include everything stored inside settings
 *
 * Created by Karol Zdebel on 7/17/2017.
 */

public class Settings {

    private final String TAG = getClass().getSimpleName();

    private int userId;
    private int carId;  //User settings car id
    private boolean firstCarAdded;  //Whether user ever added a car
    private Semaphore semaphore = new Semaphore(1);

    public Settings(int userId, int carId, boolean firstCarAdded) {
        Log.d(TAG,"Settings being created, userId:"+userId+", carId: "+carId+", firstCarId: "+firstCarAdded);
        this.userId = userId;
        this.carId = carId;
        this.firstCarAdded = firstCarAdded;
    }

    public Settings(int userId, boolean firstCarAdded){
        Log.d(TAG,"Settings being created, userId:"+userId+", firstCarId: "+firstCarAdded);
        this.userId = userId;
        this.firstCarAdded = firstCarAdded;
        carId = -1;
    }

    public void setCarId(int carId) {
        Log.d(TAG,"setCarId() carId: "+carId);
        try{
            semaphore.acquire();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        this.carId = carId;
        semaphore.release();
    }

    public void setFirstCarAdded(boolean firstCarAdded) {
        Log.d(TAG,"setFirstCarAdded() firstCarAdded: "+firstCarAdded);
        this.firstCarAdded = firstCarAdded;
    }

    public boolean hasMainCar(){
        return carId != -1;
    }

    public int getCarId() {
        try{
            semaphore.acquire();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        Log.d(TAG,"getCardId() carId: "+carId);
        semaphore.release();
        return carId;
    }

    public boolean isFirstCarAdded() {
        return firstCarAdded;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString(){
        try{
            return "userId: "+userId+", carId: "+carId+", firstCarAdded: "+firstCarAdded;
        }catch(NullPointerException e){
            return "null";
        }
    }
}

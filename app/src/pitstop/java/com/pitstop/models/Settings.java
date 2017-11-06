package com.pitstop.models;

/**
 * Not complete, doesn't include everything stored inside settings
 *
 * Created by Karol Zdebel on 7/17/2017.
 */

public class Settings {

    private int userId;
    private int carId;  //User settings car id
    private boolean firstCarAdded;  //Whether user ever added a car

    public Settings(int userId, int carId, boolean firstCarAdded) {
        this.userId = userId;
        this.carId = carId;
        this.firstCarAdded = firstCarAdded;
    }

    public Settings(int userId, boolean firstCarAdded){
        this.userId = userId;
        this.firstCarAdded = firstCarAdded;
        carId = -1;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public void setFirstCarAdded(boolean firstCarAdded) {
        this.firstCarAdded = firstCarAdded;
    }

    public boolean hasMainCar(){
        return carId != -1;
    }

    public int getCarId() {
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

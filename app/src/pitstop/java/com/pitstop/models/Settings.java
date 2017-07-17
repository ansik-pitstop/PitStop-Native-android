package com.pitstop.models;

/**
 * Not complete, doesn't include everything stored inside settings
 *
 * Created by Karol Zdebel on 7/17/2017.
 */

public class Settings {

    private int carId;  //User settings car id
    private boolean firstCarAdded;  //Whether user ever added a car

    public Settings(int carId, boolean firstCarAdded) {
        this.carId = carId;
        this.firstCarAdded = firstCarAdded;
    }

    public Settings(boolean firstCarAdded){
        this.firstCarAdded = firstCarAdded;
        carId = -1;
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

}

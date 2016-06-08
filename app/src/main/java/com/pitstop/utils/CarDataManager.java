package com.pitstop.utils;

import com.pitstop.DataAccessLayer.DTOs.Car;

/**
 * Created by Ben Wu on 2016-06-03.
 */

// This is a singleton class to allow access to the dashboard car without having to pass it around
//   as a serializable
public class CarDataManager {

    private static CarDataManager instance = null;
    private Car dashboardCar = null;

    public static CarDataManager getInstance() {
        if(instance == null) {
            instance = new CarDataManager();
        }
        return instance;
    }

    public Car getDashboardCar() {
        return dashboardCar;
    }

    public void setDashboardCar(Car car) {
        dashboardCar = car;
    }

}

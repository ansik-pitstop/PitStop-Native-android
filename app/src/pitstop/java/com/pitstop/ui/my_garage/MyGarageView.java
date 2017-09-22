package com.pitstop.ui.my_garage;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;

import java.util.List;

/**
 * Created by ishan on 2017-09-19.
 */

public interface MyGarageView {


    void openMyAppointments();
    void openRequestService();
    void toast(String message);
    boolean isUserNull();
    String getUserPhone();
    String getUserFirstName();
    String getUserEmail();
    void openSmooch();
    void callDealership(Dealership dealership);
    void showDealershipsCallDialog(List<Dealership> dealerships);
    void showDealershipsDirectionDialog(List<Dealership> dealerships);
    void openDealershipDirections(Dealership dealership);
    void onCarClicked(Car car);

    void showCars(List<Car> carList);
}

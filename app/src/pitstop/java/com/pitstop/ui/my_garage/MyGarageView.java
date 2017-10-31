package com.pitstop.ui.my_garage;

import com.pitstop.models.Dealership;
import com.pitstop.models.Car;

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
    void onCarClicked(Car car, Dealership dealership, int position);
    void showCars(List<Car> carList);
    void openSpecsActivity(Car car, Dealership dealership, int position);
    void noCarsView();
    void onUpdateNeeded();
    void showLoading();
    void hideLoading();
    void appointmentsVisible();
    void appointmentsInvisible();
    void showLoadingDialog(String s);
    void hideLoadingDialog();
    void notifyCarDataChanged();
    boolean hasBeenPopulated();

    void showErrorDialog();
}

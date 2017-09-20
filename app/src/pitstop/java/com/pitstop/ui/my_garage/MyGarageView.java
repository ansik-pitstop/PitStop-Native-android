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
    void showDealershipsDialog(List<Dealership> dealerships);
}

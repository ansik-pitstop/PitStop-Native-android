package com.pitstop.ui.my_garage;

import com.pitstop.models.Car;

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
    void callDealership(Car car);
}

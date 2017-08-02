package com.pitstop.ui.add_car.vin_entry;

import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface VinEntryView {
    void onValidVinInput();
    void onInvalidVinInput();
    int getMileage();
    void onInvalidMileage();
    void onGotDeviceInfo(String scannerId, String scannerName);
    void onCarAddedWithShop(Car car);
    void onCarAddedWithoutShop(Car car);
    void onErrorAddingCar(String message);
}

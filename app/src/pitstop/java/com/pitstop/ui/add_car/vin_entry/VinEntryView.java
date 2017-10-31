package com.pitstop.ui.add_car.vin_entry;

import com.pitstop.ui.LoadingView;
import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface VinEntryView extends LoadingView{
    void onValidVinInput();
    void onInvalidVinInput();
    String getMileage();
    void onInvalidMileage();
    void onGotDeviceInfo(String scannerId, String scannerName, int mileage);
    void onCarAddedWithShop(Car car);
    void onCarAddedWithoutShop(Car car);
    void onErrorAddingCar(String message);
    void onCarAlreadyAdded(Car car);
    void displayVin(String vin);
    void displayMileage(int mileage);
    void displayScannedVinValid();
    void displayScannedVinInvalid();
    void showAskHasDeviceView();
    void beginPendingAddCarActivity(String vin, double mileage, String scannerId);
    int getTransferredMileage();
    String getVin();
    String getScannerId();
    String getScannerName();
}

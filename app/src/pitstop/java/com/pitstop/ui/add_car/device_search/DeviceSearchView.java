package com.pitstop.ui.add_car.device_search;

import com.pitstop.ui.LoadingView;
import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface DeviceSearchView extends LoadingView{

    void onVinRetrievalFailed(String scannerName, String scannerId, int mileage);
    void onCannotFindDevice();
    String getMileage();
    void onMileageInvalid();
    void onCarAddedWithShop(Car car);
    void onCarAddedWithoutShop(Car car);
    void onErrorAddingCar(String message);
    void onCarAlreadyAdded(Car car);
    void showAskHasDeviceView();
    void beginPendingAddCarActivity(String vin, double mileage, String scannerId);

}

package com.pitstop.ui.add_car.device_search;

import com.pitstop.models.Car;
import com.pitstop.ui.LoadingView;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface DeviceSearchView extends LoadingView {

    void onVinRetrievalFailed(String scannerName, String scannerId);
    void onCannotFindDevice();
    int getMileage();
    void onMileageInvalid();
    void onCarAddedWithShop(Car car);
    void onCarAddedWithoutShop(Car car);
    void onErrorAddingCar(String message);

}

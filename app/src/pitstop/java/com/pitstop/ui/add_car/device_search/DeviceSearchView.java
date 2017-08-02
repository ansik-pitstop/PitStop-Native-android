package com.pitstop.ui.add_car.device_search;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface DeviceSearchView {

    void onVinRetrievalFailed(String scannerName, String scannerId);
    void onCannotFindDevice();
    int getMileage();
    void onMileageInvalid();

}

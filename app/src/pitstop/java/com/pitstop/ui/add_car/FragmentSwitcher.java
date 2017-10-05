package com.pitstop.ui.add_car;

import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface FragmentSwitcher {
    void setViewAskHasDevice();
    void setViewDeviceSearch();
    void setViewVinEntry(String scannerId, String scannerName, int mileage);
    void setViewVinEntry();
    void endAddCarSuccess(Car car, boolean hasDealership);
    void endAddCarFailure();
}

package com.pitstop.ui.vehicle_specs;

import com.pitstop.models.Car;

/**
 * Created by ishan on 2017-09-25.
 */

public interface VehicleSpecsView {

    void showLicensePlate(String s);
    void toast(String message);
    void showImage(String s);
    void showDealershipBanner();

    void showImageLoading();
    void hideImageLoading();

    void showLoading();
    void hideLoading();

    void showLoadingDialog(String message);
    void hideLoadingDialog();
    void setCarView(Car car);

    void showBuyDeviceDialog();
    void showNoCarView();
    void showOfflineErrorView();
    void showUnknownErrorView();
}

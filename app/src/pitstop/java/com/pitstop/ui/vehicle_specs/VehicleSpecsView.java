package com.pitstop.ui.vehicle_specs;

/**
 * Created by ishan on 2017-09-25.
 */

public interface VehicleSpecsView {

    void showLicensePlate(String s);
    void toast(String message);
    void showImage(String s);
    void showDealershipBanner();
    void closeSpecsFragment();
}

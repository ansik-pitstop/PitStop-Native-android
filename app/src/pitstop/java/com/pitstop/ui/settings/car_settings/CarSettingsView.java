package com.pitstop.ui.settings.car_settings;

import com.pitstop.models.Dealership;
import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by Matthew on 2017-06-13.
 */

public interface CarSettingsView {
    void setData(Car car, Dealership dealership);
    void setSwitcher(FragmentSwitcher switcher);
    void showCarText(String name, String shop);
    void update();
    Car getCar();
    void showDelete();
    void toast(String message);
}

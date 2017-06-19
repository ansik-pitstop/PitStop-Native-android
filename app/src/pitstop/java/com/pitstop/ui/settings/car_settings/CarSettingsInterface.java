package com.pitstop.ui.settings.car_settings;

import com.pitstop.models.Car;
import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by xirax on 2017-06-13.
 */

public interface CarSettingsInterface {
    void setCar(Car car);
    void setSwitcher(FragmentSwitcher switcher);
    Car getCar();
    void showDelete();
    void startCustomShops();
}

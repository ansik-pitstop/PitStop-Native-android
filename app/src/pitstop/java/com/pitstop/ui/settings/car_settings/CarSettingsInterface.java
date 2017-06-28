package com.pitstop.ui.settings.car_settings;

import com.pitstop.models.Car;
import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by xirax on 2017-06-13.
 */

public interface CarSettingsInterface {
    void setCar(Car car);
    void setSwitcher(FragmentSwitcher switcher);
    void showCarText(String name, String shop);
    void update();
    Car getCar();
    void showDelete();
}

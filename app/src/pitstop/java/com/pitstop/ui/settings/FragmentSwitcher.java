package com.pitstop.ui.settings;

import com.pitstop.models.Dealership;
import com.pitstop.models.Car;

/**
 * Created by Matt on 2017-06-12.
 */

public interface FragmentSwitcher {
    void setViewMainSettings();
    void setViewShopSettings();
    void setViewShopForm(Dealership dealership);
    void loading(boolean show);
    void startAddCar();
    void startCustomShops(Car car);
}

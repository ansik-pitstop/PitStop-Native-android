package com.pitstop.ui.settings;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;

/**
 * Created by Matt on 2017-06-12.
 */

public interface FragmentSwitcher {
    void setViewMainSettings();
    void setViewCarSettings();
    void setViewShopSettings();
    void setViewShopForm(Dealership dealership);
    void loading(boolean show);
    void startAddCar();
    void startCustomShops(Car car);
}

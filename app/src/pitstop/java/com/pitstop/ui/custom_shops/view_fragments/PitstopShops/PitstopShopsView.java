package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;

import java.util.List;

/**
 * Created by Matt on 2017-06-08.
 */

public interface PitstopShopsView {
    void setSwitcher(CustomShopActivityCallback switcher);
    void focusSearch();
    void showDealershipList(List<Dealership> dealerships);
    void showConfirmation(Dealership dealership);
    Car getCar();
    void loading(boolean show);
    void toast(String message);
}

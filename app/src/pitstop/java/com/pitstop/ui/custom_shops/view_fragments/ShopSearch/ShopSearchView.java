package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import com.google.android.gms.maps.model.LatLng;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;

import java.util.List;

/**
 * Created by matt on 2017-06-08.
 */

public interface ShopSearchView {
    void setSwitcher(CustomShopActivityCallback switcher);
    void focusSearch();
    void unFocusSearch();
    void showConfirmation(Dealership dealership);
    Car getCar();
    void showShopCategory(boolean show);
    void showSearchCategory(boolean show);
    void showPitstopCategory(boolean show);
    void setUpPitstopList(List<Dealership> dealerships);
    void setUpSearchList(List<Dealership> dealerships);
    void setUpMyShopsList(List<Dealership> dealerships);
    LatLng getLocation();
    void loadingGoogle(boolean show);
    void loadingMyShops(boolean show);
    void toast(String message);
}

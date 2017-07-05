package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by Matt on 2017-06-09.
 */

public interface ShopFormView {
    void setSwitcher(CustomShopActivityCallback switcher);
    void setSwitcher(FragmentSwitcher switcher);
    void setDealership(Dealership dealership);
    void showName(String name);
    void showPhone(String phone);
    void showEmail(String email);
    void showAddress(String address);
    void showCity(String city);
    void showProvince(String province);
    void showPostal(String postal);
    void showCountry(String country);
    void showReminder(String message);
    void setCar(Car car);
    void showError();
    Car getCar();
    Dealership getDealership();
    String getName();
    String getPhone();
    String getEmail();
    String getAddress();
    String getCity();
    String getProvince();
    String getPostal();
    String getCountry();
    void toast(String message);
}

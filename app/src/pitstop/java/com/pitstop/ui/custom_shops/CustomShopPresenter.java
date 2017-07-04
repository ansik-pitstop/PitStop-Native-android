package com.pitstop.ui.custom_shops;

/**
 * Created by matt on 2017-06-08.
 */

public class CustomShopPresenter {
    private CustomShopInterface customShop;
    private CustomShopActivityCallback fragmentSwitcher;
    public void subscribe(CustomShopActivityCallback fragmentSwitcher, CustomShopInterface customShop){
        this.customShop = customShop;
        this.fragmentSwitcher = fragmentSwitcher;

    }
    public void setViewCustomShop(){
        fragmentSwitcher.setViewShopType();
    }

    public void setUpNavBar(){
        customShop.setUpNavBar();

    }
}

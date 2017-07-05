package com.pitstop.ui.custom_shops;

/**
 * Created by matt on 2017-06-08.
 */

public class CustomShopPresenter {
    private CustomShopView customShop;
    private CustomShopActivityCallback fragmentSwitcher;

    public CustomShopPresenter(CustomShopActivityCallback fragmentSwitcher){
        this.fragmentSwitcher = fragmentSwitcher;
    }

    public void subscribe(CustomShopView customShop){
        this.customShop = customShop;
    }
    public void setViewCustomShop(){
        fragmentSwitcher.setViewShopType();
    }

    public void setUpNavBar(){
        customShop.setUpNavBar();

    }
}

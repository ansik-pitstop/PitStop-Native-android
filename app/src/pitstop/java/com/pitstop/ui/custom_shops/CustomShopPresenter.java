package com.pitstop.ui.custom_shops;

import com.pitstop.utils.MixpanelHelper;

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
    public void unsubscribe(){
        this.customShop = null;
    }
    public void setViewCustomShop(){
        if(customShop == null){return;}
        fragmentSwitcher.setViewShopType();
    }

    public void setUpNavBar(){
        if(customShop == null){return;}
        customShop.setUpNavBar();

    }


}

package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypePresenter {
    private ShopTypeInterface shopTypeFragment;
    private FragmentSwitcherInterface switcher;
    public void subscribe(ShopTypeInterface shopTypeInterface, FragmentSwitcherInterface switcher){
        this.shopTypeFragment = shopTypeInterface;
        this.switcher = switcher;
    }
    public void setViewShopSearch(){
        switcher.setViewSearchShop();
    }

    public void setViewPitstopShops(){
        switcher.setViewPitstopShops();
    }
    public void showNoShopWarning(){
        shopTypeFragment.noShopWarning();
    }
}

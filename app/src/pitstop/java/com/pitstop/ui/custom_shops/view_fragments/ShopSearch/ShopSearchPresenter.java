package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.custom_shops.ShopPresnter;

/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchPresenter implements ShopPresnter {
    private ShopSearchInterface shopSearch;
    private FragmentSwitcherInterface switcher;
    public void subscribe(ShopSearchInterface shopSearch, FragmentSwitcherInterface switcher){
        this.shopSearch = shopSearch;
        this.switcher = switcher;
    }
    public void focusSearch(){
        shopSearch.focusSearch();
    }
    public void setViewShopForm(Dealership dealership){
        switcher.setViewShopForm(dealership);
    }

    @Override
    public void onShopClicked(Dealership dealership) {

    }

    public void getMyShops(){

    }
}

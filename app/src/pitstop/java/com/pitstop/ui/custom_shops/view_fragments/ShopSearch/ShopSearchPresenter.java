package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchPresenter {
    private ShopSearchInterface shopSearch;
    private FragmentSwitcherInterface switcher;
    public void subscribe(ShopSearchInterface shopSearch, FragmentSwitcherInterface switcher){
        this.shopSearch = shopSearch;
        this.switcher = switcher;
    }
    public void focusSearch(){
        shopSearch.focusSearch();
    }
}

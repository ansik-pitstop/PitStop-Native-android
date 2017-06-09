package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

/**
 * Created by xirax on 2017-06-09.
 */

public class ShopFormPresenter {
    private ShopFormInterface shopForm;
    private FragmentSwitcherInterface switcher;
    public void subscribe(ShopFormInterface shopForm, FragmentSwitcherInterface switcher){
        this.shopForm = shopForm;
        this.switcher = switcher;
    }
}

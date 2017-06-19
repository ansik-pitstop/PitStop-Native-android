package com.pitstop.ui.custom_shops;

import android.app.Fragment;

import com.pitstop.models.Dealership;

/**
 * Created by matt on 2017-06-08.
 */

public interface FragmentSwitcherInterface {
    void setViewShopType();
    void setViewSearchShop();
    void setViewPitstopShops();
    void setViewShopForm(Dealership dealership);

}

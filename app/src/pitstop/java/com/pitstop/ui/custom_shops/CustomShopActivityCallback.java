package com.pitstop.ui.custom_shops;

import com.pitstop.models.Dealership;

/**
 * Created by matt on 2017-06-08.
 */

public interface CustomShopActivityCallback {
    void setViewShopType();
    void setViewSearchShop();
    void setViewPitstopShops();
    void setViewShopForm(Dealership dealership);
    void endCustomShops();
}

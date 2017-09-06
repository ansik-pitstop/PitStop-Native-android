package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import com.pitstop.ui.custom_shops.CustomShopActivityCallback;

/**
 * Created by matt on 2017-06-07.
 */

public interface ShopTypeView {
    void setSwitcher(CustomShopActivityCallback switcher);
    void noShopWarning();
    void displayError(String message);
}

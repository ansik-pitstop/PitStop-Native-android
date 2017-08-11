package com.pitstop.ui.custom_shops;

/**
 * Created by matt on 2017-06-07.
 */

public interface CustomShopView {

    String VIEW_SHOP_PITSTOP = "view_shop_pitstop";
    String VIEW_SHOP_FORM = "view_shop_form";
    String VIEW_SHOP_TYPE = "view_shop_type";
    String VIEW_SHOP_SEARCH = "view_shop_search";

    void setUpNavBar(boolean homeButton);
    void back();
    String getCurrentViewName();
}

package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

import java.util.List;

/**
 * Created by matt on 2017-06-08.
 */

public interface ShopSearchInterface {
    void setSwitcher(FragmentSwitcherInterface switcher);
    void focusSearch();
    void setUpPitstopList(List<Dealership> dealerships);
    void setUpSearchList(List<Dealership> dealerships);
    void setUpMyShopsList(List<Dealership> dealerships);
}

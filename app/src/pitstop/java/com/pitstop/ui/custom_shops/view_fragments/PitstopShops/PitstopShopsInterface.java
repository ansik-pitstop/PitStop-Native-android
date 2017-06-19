package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

import java.util.List;

/**
 * Created by xirax on 2017-06-08.
 */

public interface PitstopShopsInterface {
    void setSwitcher(FragmentSwitcherInterface switcher);
    void focusSearch();
    void setupList(List<Dealership> dealerships);
}

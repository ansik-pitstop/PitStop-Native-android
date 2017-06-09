package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

/**
 * Created by xirax on 2017-06-08.
 */

public class PitstopShopsPresenter {
    private PitstopShopsInterface pitstopShops;
    private FragmentSwitcherInterface switcher;
    public void subscribe(PitstopShopsInterface pitstopShops, FragmentSwitcherInterface switcher){
        this.pitstopShops = pitstopShops;
        this.switcher = switcher;
    }
}

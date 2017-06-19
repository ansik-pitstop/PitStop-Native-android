package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.custom_shops.ShopTypePresnter;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by xirax on 2017-06-08.
 */

public class PitstopShopsPresenter implements ShopTypePresnter {
    @Inject
    GetPitstopShopsUseCase getPitstopShopsUseCase;

    private PitstopShopsInterface pitstopShops;
    private FragmentSwitcherInterface switcher;
    public void subscribe(PitstopShopsInterface pitstopShops, FragmentSwitcherInterface switcher){
        this.pitstopShops = pitstopShops;
        this.switcher = switcher;
    }
    public void focusSearch(){
        pitstopShops.focusSearch();
    }
    public void getShops(){
        getPitstopShopsUseCase.execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                pitstopShops.setupList(dealerships);
            }

            @Override
            public void onError() {

            }
        });

    }

    @Override
    public void onShopClicked(Dealership dealership) {
        switcher.setViewShopForm(dealership);
        System.out.println("Testing "+dealership.getName());

    }
}

package com.pitstop.ui.settings.shop_settings;

import com.pitstop.interactors.RemoveShopUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.ui.settings.FragmentSwitcher;

import javax.inject.Inject;

/**
 * Created by Matthew on 2017-06-26.
 */

public class ShopSettingsPresenter {
    private ShopSettingsInterface shopSettings;
    private FragmentSwitcher switcher;

    @Inject
    RemoveShopUseCase removeShopUseCase;

    public void subscribe(ShopSettingsInterface shopSettings, FragmentSwitcher switcher){
        this.shopSettings = shopSettings;
        this.switcher = switcher;
    }

    public void deleteClicked(){
        shopSettings.showDeleteWarning();
    }

    public void removeShop(Dealership dealership){
        removeShopUseCase.execute(dealership, new RemoveShopUseCase.Callback() {
            @Override
            public void onShopRemoved() {
                switcher.setViewMainSettings();
            }

            @Override
            public void onCantRemoveShop() {
                shopSettings.showCantDelete();
            }

            @Override
            public void onError() {
            }
        });
    }

    public void showForm(Dealership dealership){
        switcher.setViewShopForm(dealership);
    }

    public void editClicked(){

    }




}

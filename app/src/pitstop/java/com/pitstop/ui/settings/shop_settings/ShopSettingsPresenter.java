package com.pitstop.ui.settings.shop_settings;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.RemoveShopUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.ui.settings.FragmentSwitcher;


/**
 * Created by Matthew on 2017-06-26.
 */

public class ShopSettingsPresenter {
    private ShopSettingsView shopSettings;
    private FragmentSwitcher switcher;
    private UseCaseComponent component;



    public ShopSettingsPresenter(FragmentSwitcher switcher, UseCaseComponent component){
        this.switcher = switcher;
        this.component = component;
    }

    public void subscribe(ShopSettingsView shopSettings){
        this.shopSettings = shopSettings;
    }

    public void deleteClicked(){
        shopSettings.showDeleteWarning();
    }

    public void removeShop(Dealership dealership){
        component.getRemoveShopUseCase().execute(dealership, new RemoveShopUseCase.Callback() {
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
                shopSettings.toast("There was an error removing this shop");
            }
        });
    }

    public void showForm(Dealership dealership){
        switcher.setViewShopForm(dealership);
    }

}

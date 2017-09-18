package com.pitstop.ui.settings.shop_settings;

import android.content.res.Resources;

import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matthew on 2017-06-26.
 */

public class ShopSettingsPresenter {
    private ShopSettingsView shopSettings;
    private FragmentSwitcher switcher;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;



    public ShopSettingsPresenter(FragmentSwitcher switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(ShopSettingsView shopSettings){
        mixpanelHelper.trackViewAppeared("ShopSettings");
        this.shopSettings = shopSettings;
    }

    public void unsubscribe(){
        this.shopSettings = null;
    }

    public void deleteClicked(){
        if(shopSettings == null){return;}
        mixpanelHelper.trackButtonTapped("DeleteShop","ShopSettings");
        shopSettings.showDeleteWarning();
    }

    public void removeShop(Dealership dealership){
        if(shopSettings == null || switcher == null){return;}
        component.getRemoveShopUseCase().execute(dealership, new RemoveShopUseCase.Callback() {
            @Override
            public void onShopRemoved() {
                if(shopSettings != null && switcher != null){
                    switcher.setViewMainSettings();
                }
            }

            @Override
            public void onCantRemoveShop() {
                if(shopSettings != null){
                    shopSettings.showCantDelete();
                }
            }

            @Override
            public void onError(RequestError error) {
                if(shopSettings != null){
                    shopSettings.toast(Resources.getSystem().getString(R.string.error_removing_shop_toast));
                }
            }
        });
    }

    public void showForm(Dealership dealership){
        if(shopSettings == null || switcher == null){return;}
        mixpanelHelper.trackButtonTapped("EditShop","ShopSettings");
        switcher.setViewShopForm(dealership);
    }

}

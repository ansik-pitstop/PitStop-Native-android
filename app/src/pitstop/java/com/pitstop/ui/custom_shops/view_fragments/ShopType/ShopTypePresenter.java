package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypePresenter {

    private ShopTypeView shopTypeFragment;
    private CustomShopActivityCallback switcher;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;

    public ShopTypePresenter(CustomShopActivityCallback switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(ShopTypeView shopTypeView){
        mixpanelHelper.trackViewAppeared("ShopTypeSelection");
        this.shopTypeFragment = shopTypeView;

    }

    public void unsubscribe(){
        this.shopTypeFragment = null;
    }
    public void setViewShopSearch(){
        if(shopTypeFragment == null){return;}
        mixpanelHelper.trackButtonTapped("ShopSearch","ShopTypeSelection");
        switcher.setViewSearchShop();
    }

    public void setViewPitstopShops(){
        if(shopTypeFragment == null){return;}
        mixpanelHelper.trackButtonTapped("PitstopShops","ShopTypeSelection");
        switcher.setViewPitstopShops();
    }
    public void showNoShopWarning(){
        if(shopTypeFragment == null){return;}
        mixpanelHelper.trackButtonTapped("NoShop","ShopTypeSelection");
        shopTypeFragment.noShopWarning();
    }

    public void setCarNoDealer(Car car){
        if(shopTypeFragment == null){return;}
        mixpanelHelper.trackButtonTapped("NoShopConfirm","ShopTypeSelection");
        if(car == null){
            return;
        }
        Dealership noDealer = new Dealership();
        if(BuildConfig.DEBUG){
            noDealer.setId(1);
        }else {
            noDealer.setId(19);
        }
        component.getUpdateCarDealershipUseCase().execute(car.getId(), noDealer, EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                if(shopTypeFragment != null){
                    switcher.endCustomShops();
                }
            }

            @Override
            public void onError() {

            }
        });

    }
}

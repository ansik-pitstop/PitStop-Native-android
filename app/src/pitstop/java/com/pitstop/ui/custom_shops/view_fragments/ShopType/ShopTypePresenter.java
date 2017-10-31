package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.models.Car;


/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypePresenter {

    private ShopTypeView view;
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
        this.view = shopTypeView;

    }

    public void unsubscribe(){
        this.view = null;
    }
    public void setViewShopSearch(){
        if(view == null){return;}
        mixpanelHelper.trackButtonTapped("ShopSearch","ShopTypeSelection");
        switcher.setViewSearchShop();
    }

    public void setViewPitstopShops(){
        if(view == null){return;}
        mixpanelHelper.trackButtonTapped("PitstopShops","ShopTypeSelection");
        switcher.setViewPitstopShops();
    }
    public void showNoShopWarning(){
        if(view == null){return;}
        mixpanelHelper.trackButtonTapped("NoShop","ShopTypeSelection");
        view.noShopWarning();
    }

    public void setCarNoDealer(Car car){
        if(view == null){return;}
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
                if(view != null){
                    switcher.endCustomShops("No Shop");
                }
            }

            @Override
            public void onError(RequestError error) {
                if (view != null){
                    view.displayError(error.getMessage());
                }
            }
        });

    }
}

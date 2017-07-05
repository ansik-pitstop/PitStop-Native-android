package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import com.pitstop.BuildConfig;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;


/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypePresenter {

    private ShopTypeView shopTypeFragment;
    private CustomShopActivityCallback switcher;
    private UseCaseComponent component;

    private static final int NO_SHOP_DEBUG = 1;
    private static final int NO_SHOP_PROD = 19;

    public ShopTypePresenter(CustomShopActivityCallback switcher, UseCaseComponent component){
        this.switcher = switcher;
        this.component = component;
    }

    public void subscribe(ShopTypeView shopTypeView){
        this.shopTypeFragment = shopTypeView;

    }
    public void setViewShopSearch(){
        switcher.setViewSearchShop();
    }

    public void setViewPitstopShops(){
        switcher.setViewPitstopShops();
    }
    public void showNoShopWarning(){
        shopTypeFragment.noShopWarning();
    }

    public void setCarNoDealer(Car car){
        if(car == null){
            return;
        }
        Dealership noDealer = new Dealership();
        if(BuildConfig.DEBUG){
            noDealer.setId(NO_SHOP_DEBUG);
        }else {
            noDealer.setId(NO_SHOP_PROD);
        }
        component.getUpdateCarDealershipUseCase().execute(car.getId(), noDealer, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                switcher.endCustomShops();
            }

            @Override
            public void onError() {

            }
        });

    }
}

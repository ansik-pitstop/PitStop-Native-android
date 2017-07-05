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

    private ShopTypeInterface shopTypeFragment;
    private CustomShopActivityCallback switcher;
    private UseCaseComponent component;

    public ShopTypePresenter(CustomShopActivityCallback switcher, UseCaseComponent component){
        this.switcher = switcher;
        this.component = component;
    }

    public void subscribe(ShopTypeInterface shopTypeInterface){
        this.shopTypeFragment = shopTypeInterface;

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
            noDealer.setId(1);
        }else {
            noDealer.setId(19);
        }
        component.getUpdateCarDealershipUseCase().execute(car.getId(), noDealer, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                switcher.endActivity();
            }

            @Override
            public void onError() {

            }
        });

    }
}

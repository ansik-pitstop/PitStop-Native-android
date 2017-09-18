package com.pitstop.ui.custom_shops;

import android.app.Activity;
import android.content.res.Resources;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.add_car.AddCarActivity;

/**
 * Created by matt on 2017-06-08.
 */

public class CustomShopPresenter {
    private CustomShopView customShop;
    private CustomShopActivityCallback fragmentSwitcher;
    private UseCaseComponent component;
    private String startSource;

    private final static int DEBUG_NO_DEALER = 1;
    private final static int PROD_NO_DEALER = 19;


    public CustomShopPresenter(CustomShopActivityCallback fragmentSwitcher
            , UseCaseComponent component, String startSource){
        this.fragmentSwitcher = fragmentSwitcher;
        this.component = component;
        this.startSource = startSource;
    }

    public void setNoDealer(Car car){
        if(customShop == null){return;}
        Dealership dealership = new Dealership();
        dealership.setName(((Activity)customShop).getString(R.string.dealership_not_found));
        if(BuildConfig.DEBUG){
            dealership.setId(DEBUG_NO_DEALER);
        }else{
            dealership.setId(PROD_NO_DEALER);
        }
        component.getUpdateCarDealershipUseCase().execute(car.getId(), dealership, EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                if(customShop != null){
                    customShop.back();
                }
            }

            @Override
            public void onError(RequestError error) {
            }
        });

    }

    public void subscribe(CustomShopView customShop){
        this.customShop = customShop;
    }

    public void unsubscribe(){
        this.customShop = null;
    }

    public void setViewCustomShop(){
        if(customShop == null){return;}
        fragmentSwitcher.setViewShopType();
    }

    public void setUpNavBar(){
        if(customShop == null){return;}

        //Show home button only if this activity hasn't been started from AddCar
        customShop.setUpNavBar(!startSource.equals(AddCarActivity.class.getName()));

    }

    //Returns true if it handled the back press, false otherwise
    public boolean onBackPressed(){

        //Return true, meaning DO NOT handle this back press IF selecting a shop
        // and Add Car was the previous activity
        if (startSource.equals(AddCarActivity.class.getName())
                && customShop.getCurrentViewName().equals(CustomShopView.VIEW_SHOP_TYPE)){

            return true;
        }

        return false;
    }


}

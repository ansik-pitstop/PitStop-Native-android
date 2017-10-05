package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopPresenter;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;


/**
 * Created by matt on 2017-06-08.
 */

class ShopSearchPresenter implements ShopPresenter {

    private final String TAG = getClass().getSimpleName();

    private ShopSearchView view;
    private CustomShopActivityCallback switcher;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;

    private int loadingCounter;

    ShopSearchPresenter(CustomShopActivityCallback switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(ShopSearchView shopSearch){
        Log.d(TAG,"subscribe()");
        mixpanelHelper.trackViewAppeared("ShopSearch");
        loadingCounter = 0;
        this.view = shopSearch;
    }
    public void unsubscribe() {
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }
    void focusSearch(){
        Log.d(TAG,"focusSearch()");
        if(view == null){return;}
        view.focusSearch();
    }

    @Override
    public void onShopClicked(Dealership dealership) {
        Log.d(TAG,"onShopClicked() dealership: "+dealership.getName());
        if(view == null){return;}
        if(dealership.isCustom()){
            if(dealership.getGooglePlaceId()!= null){
                mixpanelHelper.trackButtonTapped("GooglePlacesShop","ShopSearch");
                component.getGetPlaceDetailsUseCase().execute(dealership, new GetPlaceDetailsUseCase.Callback() {
                    @Override
                    public void onDetailsGot(Dealership dealership) {
                        if(view != null){
                            switcher.setViewShopForm(dealership);
                        }
                    }
                    @Override
                    public void onError(RequestError error) {//go to the shop form without extra details
                        if(view != null){
                            switcher.setViewShopForm(dealership);
                        }
                    }
                });

            }else{
                mixpanelHelper.trackButtonTapped("UserShop","ShopSearch");
                switcher.setViewShopForm(dealership);
            }
        }else{
            mixpanelHelper.trackButtonTapped("PitstopShop","ShopSearch");
           view.showConfirmation(dealership);
        }
    }

    void changeShop(Dealership dealership){
        Log.d(TAG,"changeShop() dealership: "+dealership);
        if(view == null){return;}
        Car car = view.getCar();
        component.getUpdateCarDealershipUseCase().execute(car.getId(), dealership, EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                if(view != null){
                    switcher.endCustomShops();
                }
            }

            @Override
            public void onError(RequestError error) {
            }
        });
    }

    void filterLists(String filter){//search for filter
        Log.d(TAG,"filterLists() filter: "+filter);
        if(view == null){return;}
        filter = filter.toLowerCase();

        LatLng location = view.getLocation();
        if(location == null){
            return;
        }
        view.loadingGoogle(true);
        loadingCounter+=1;
        component.getGetGooglePlacesShopsUseCase().execute(location.latitude,location.longitude
                , filter, new GetGooglePlacesShopsUseCase.CallbackShops() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                Log.d(TAG,"onShopsGot() dealerships size: "+dealerships.size());
                if(view != null){
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        view.loadingGoogle(false);
                    }
                    view.setUpSearchList(dealerships);
                }
            }
            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getGooglePlacesShops onError() error: "+error.getMessage());
                if(view != null){
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        view.loadingGoogle(false);
                    }
                }
            }
        });
    }
}

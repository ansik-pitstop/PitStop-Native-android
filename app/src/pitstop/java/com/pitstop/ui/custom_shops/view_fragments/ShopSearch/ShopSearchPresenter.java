package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopPresnter;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by matt on 2017-06-08.
 */

class ShopSearchPresenter implements ShopPresnter {

    private final String TAG = getClass().getSimpleName();

    private ShopSearchView shopSearch;
    private CustomShopActivityCallback switcher;
    private UseCaseComponent component;

    private List<Dealership> pitstopShops;

    private MixpanelHelper mixpanelHelper;

    private boolean emptySearch;

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
        this.shopSearch = shopSearch;
    }
    public void unsubscribe() {
        Log.d(TAG,"unsubscribe()");
        this.shopSearch = null;
    }
    void focusSearch(){
        Log.d(TAG,"focusSearch()");
        if(shopSearch == null){return;}
        shopSearch.focusSearch();
    }

    @Override
    public void onShopClicked(Dealership dealership) {
        Log.d(TAG,"onShopClicked() dealership: "+dealership.getName());
        if(shopSearch == null){return;}
        if(dealership.isCustom()){
            if(dealership.getGooglePlaceId()!= null){
                mixpanelHelper.trackButtonTapped("GooglePlacesShop","ShopSearch");
                component.getGetPlaceDetailsUseCase().execute(dealership, new GetPlaceDetailsUseCase.Callback() {
                    @Override
                    public void onDetailsGot(Dealership dealership) {
                        if(shopSearch != null){
                            switcher.setViewShopForm(dealership);
                        }
                    }
                    @Override
                    public void onError(RequestError error) {//go to the shop form without extra details
                        if(shopSearch != null){
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
           shopSearch.showConfirmation(dealership);
        }
    }

    void changeShop(Dealership dealership){
        Log.d(TAG,"changeShop() dealership: "+dealership);
        if(shopSearch == null){return;}
        Car car = shopSearch.getCar();
        component.getUpdateCarDealershipUseCase().execute(car.getId(), dealership, EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                if(shopSearch != null){
                    switcher.endCustomShops();
                }
            }

            @Override
            public void onError(RequestError error) {


            }
        });
    }

    void loadNearbyShops(){
        Log.d(TAG,"load nearby shops()");
        LatLng location = shopSearch.getLocation();
        if(location == null){
            return;
        }
        shopSearch.loadingGoogle(true);
        loadingCounter+=1;
        component.getGetGooglePlacesShopsUseCase().execute(location.latitude,location.longitude, null
                , new GetGooglePlacesShopsUseCase.CallbackShops() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                if(shopSearch != null){
                    shopSearch.showSearchCategory(dealerships.size()>0 && !emptySearch);
                    shopSearch.setUpSearchList(dealerships);
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        shopSearch.loadingGoogle(false);
                    }
                }
            }
            @Override
            public void onError(RequestError error) {
                if(shopSearch != null){
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        shopSearch.loadingGoogle(false);
                    }
                }
            }
        });
    }

    void filterLists(String filter){//search for filter
        Log.d(TAG,"filterLists() filter: "+filter);
        if(shopSearch == null){return;}
        filter = filter.toLowerCase();
        emptySearch = filter.equals("");
        if(pitstopShops != null){
           List<Dealership> dealerships = new ArrayList<>();
            for(Dealership d:pitstopShops){
                String name = d.getName().toLowerCase();
                String address = d.getAddress().toLowerCase();
                if(name.contains(filter)||address.contains(filter)){
                    dealerships.add(d);
                }

            }
           shopSearch.showPitstopCategory(dealerships.size()>0 && !emptySearch);
           shopSearch.setUpPitstopList(dealerships);
        }

        if(emptySearch){
            shopSearch.showSearchCategory(false);
            return;
        }


        LatLng location = shopSearch.getLocation();
        if(location == null){
            return;
        }
        shopSearch.loadingGoogle(true);
        loadingCounter+=1;
        component.getGetGooglePlacesShopsUseCase().execute(location.latitude,location.longitude
                , filter, new GetGooglePlacesShopsUseCase.CallbackShops() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                if(shopSearch != null){
                    shopSearch.showSearchCategory(dealerships.size()>0 && !emptySearch);
                    shopSearch.setUpSearchList(dealerships);
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        shopSearch.loadingGoogle(false);
                    }
                }
            }
            @Override
            public void onError(RequestError error) {
                if(shopSearch != null){
                    loadingCounter-=1;
                    if(loadingCounter == 0){
                        shopSearch.loadingGoogle(false);
                    }
                }
            }
        });
    }

    void getPitstopShops(){
        Log.d(TAG,"getPitstopShops()");
        if(shopSearch == null){return;}
        component.getGetPitstopShopsUseCase().execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                if(shopSearch != null){
                    pitstopShops = dealerships;
                }
            }

            @Override
            public void onError(RequestError error) {
                if(shopSearch != null){
                    shopSearch.toast("There was an error loading the Pitstop shops");
                }
            }
        });

    }
}

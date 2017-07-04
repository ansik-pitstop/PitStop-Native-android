package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import com.google.android.gms.maps.model.LatLng;
import com.pitstop.interactors.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.GetPlaceDetailsUseCase;
import com.pitstop.interactors.GetUserShopsUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopPresnter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchPresenter implements ShopPresnter {
    private ShopSearchInterface shopSearch;
    private CustomShopActivityCallback switcher;

    private List<Dealership> pitstopShops;

    private boolean emptySearch;

    private int loadingCounter = 0;


    @Inject
    GetUserShopsUseCase getUserShopsUseCase;

    @Inject
    GetPitstopShopsUseCase getPitstopShopsUseCase;

    @Inject
    UpdateCarDealershipUseCase updateCarDealershipUseCase;

    @Inject
    GetGooglePlacesShopsUseCase getGooglePlacesShopsUseCase;

    @Inject
    GetPlaceDetailsUseCase getPlaceDetailsUseCase;

    public void subscribe(ShopSearchInterface shopSearch, CustomShopActivityCallback switcher){
        this.shopSearch = shopSearch;
        this.switcher = switcher;
    }
    public void focusSearch(){
        shopSearch.focusSearch();
    }

    public void setViewShopForm(Dealership dealership){
        switcher.setViewShopForm(dealership);
    }

    @Override
    public void onShopClicked(Dealership dealership) {
        if(dealership.isCustom()){
            if(dealership.getGooglePlaceId()!= null){
                getPlaceDetailsUseCase.execute(dealership, new GetPlaceDetailsUseCase.Callback() {
                    @Override
                    public void onDetailsGot(Dealership dealership) {
                        switcher.setViewShopForm(dealership);
                    }
                    @Override
                    public void onError() {//go to the shop form without extra details
                        switcher.setViewShopForm(dealership);
                    }
                });

            }else{
                switcher.setViewShopForm(dealership);
            }
        }else{
           shopSearch.showConfirmation(dealership);
        }
    }

    public void changeShop(Dealership dealership){
        Car car = shopSearch.getCar();
        updateCarDealershipUseCase.execute(car.getId(), dealership, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                switcher.endActivity();
            }

            @Override
            public void onError() {


            }
        });
    }

    public void filterLists(String filter){//search for filter
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
        shopSearch.loadingGoogle(true);
        loadingCounter+=1;
        getGooglePlacesShopsUseCase.execute(location.latitude,location.longitude, filter, new GetGooglePlacesShopsUseCase.CallbackShops() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                shopSearch.showSearchCategory(dealerships.size()>0 && !emptySearch);
                shopSearch.setUpSearchList(dealerships);
                loadingCounter-=1;
                if(loadingCounter == 0){
                    shopSearch.loadingGoogle(false);
                }
            }
            @Override
            public void onError() {
                loadingCounter-=1;
                if(loadingCounter == 0){
                    shopSearch.loadingGoogle(false);
                }
            }
        });
    }

    public void getMyShops(){
        shopSearch.loadingMyShops(true);
        getUserShopsUseCase.execute(new GetUserShopsUseCase.Callback() {
            @Override
            public void onShopGot(List<Dealership> dealerships) {
                shopSearch.showShopCategory(dealerships.size()>0);
                shopSearch.setUpMyShopsList(dealerships);
                shopSearch.loadingMyShops(false);
            }
            @Override
            public void onError() {
                shopSearch.loadingMyShops(false);
                shopSearch.toast("There was an error loading your shops");
            }
        });
    }
    public void getPitstopShops(){
        getPitstopShopsUseCase.execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                pitstopShops = dealerships;
            }

            @Override
            public void onError() {
                shopSearch.toast("There was an error loading the Pitstop shops");
            }
        });

    }
}

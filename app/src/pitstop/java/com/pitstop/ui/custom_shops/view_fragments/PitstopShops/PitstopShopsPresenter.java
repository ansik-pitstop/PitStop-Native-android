package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopPresnter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Matt on 2017-06-08.
 */

public class PitstopShopsPresenter implements ShopPresnter {

    private PitstopShopsView pitstopShops;
    private CustomShopActivityCallback switcher;
    private List<Dealership> localDealerships;
    private UseCaseComponent component;


    public PitstopShopsPresenter(CustomShopActivityCallback switcher, UseCaseComponent component){
        this.switcher = switcher;
        this.component = component;
    }

    public void subscribe(PitstopShopsView pitstopShops){
        this.pitstopShops = pitstopShops;

    }
    public void focusSearch(){
        pitstopShops.focusSearch();
    }

    public void getShops(){
        pitstopShops.loading(true);
        component.getGetPitstopShopsUseCase().execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                List<Dealership> sortedDealers = sortShops(dealerships);
                pitstopShops.showDealershipList(sortedDealers);
                localDealerships = sortedDealers;
                pitstopShops.loading(false);
            }

            @Override
            public void onError() {
                pitstopShops.loading(false);
                pitstopShops.toast("There was an error loading the Pitstop shops");
            }
        });

    }


    public List<Dealership> sortShops(List<Dealership> dealerships){
        Collections.sort(dealerships, new Comparator<Dealership>() {
            @Override
            public int compare(Dealership dealership1, Dealership dealership2)
            {

                return  dealership1.getName().compareTo(dealership2.getName());
            }
        });
        return dealerships;
    }

    public void changeShop(Dealership dealership){
        Car car = pitstopShops.getCar();

        component.getUpdateCarDealershipUseCase().execute(car.getId(), dealership, EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {

                switcher.endCustomShops();
            }

            @Override
            public void onError() {

            }
        });
    }

    public void filterShops(String filter){
        if(localDealerships == null){
            return;
        }
        List<Dealership> dealerships = new ArrayList<>();
        filter = filter.toLowerCase();
        for(Dealership d:localDealerships){
            String name = d.getName().toLowerCase();
            String address = d.getAddress().toLowerCase();
            if(name.contains(filter)||address.contains(filter)){
                dealerships.add(d);
            }
        }
        pitstopShops.showDealershipList(dealerships);
    }

    @Override
    public void onShopClicked(Dealership dealership) {
       pitstopShops.showConfirmation(dealership);
    }
}

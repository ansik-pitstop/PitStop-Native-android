package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import android.widget.Toast;

import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.custom_shops.ShopPresnter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by xirax on 2017-06-08.
 */

public class PitstopShopsPresenter implements ShopPresnter {
    @Inject
    GetPitstopShopsUseCase getPitstopShopsUseCase;

    @Inject
    UpdateCarDealershipUseCase updateCarDealershipUseCase;

    private PitstopShopsInterface pitstopShops;
    private FragmentSwitcherInterface switcher;
    private List<Dealership> localDealerships;

    public void subscribe(PitstopShopsInterface pitstopShops, FragmentSwitcherInterface switcher){
        this.pitstopShops = pitstopShops;
        this.switcher = switcher;
    }
    public void focusSearch(){
        pitstopShops.focusSearch();
    }

    public void getShops(){
        getPitstopShopsUseCase.execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                List<Dealership> sortedDealers = sortShops(dealerships);
                pitstopShops.setupList(sortedDealers);
                localDealerships = sortedDealers;
            }

            @Override
            public void onError() {

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
        pitstopShops.setupList(dealerships);
    }

    @Override
    public void onShopClicked(Dealership dealership) {
       pitstopShops.showConfirmation(dealership);
    }
}

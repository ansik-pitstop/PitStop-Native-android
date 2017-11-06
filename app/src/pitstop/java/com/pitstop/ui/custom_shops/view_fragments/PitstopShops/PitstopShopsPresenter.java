package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import android.app.Fragment;
import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopPresenter;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Matt on 2017-06-08.
 */

public class PitstopShopsPresenter implements ShopPresenter {

    private final String TAG = getClass().getSimpleName();

    private PitstopShopsView view;
    private CustomShopActivityCallback switcher;
    private List<Dealership> localDealerships;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;


    public PitstopShopsPresenter(CustomShopActivityCallback switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(PitstopShopsView pitstopShops){
        Log.d(TAG,"subscribe() view: "+pitstopShops);
        mixpanelHelper.trackViewAppeared("PitstopShops");
        this.view = pitstopShops;
    }
    public void unsubscribe(){
        this.view = null;
    }
    public void focusSearch(){
        if(view == null){return;};
        view.focusSearch();
    }

    public void getShops(){
        if(view == null ){return;}
        view.loading(true);
        component.getGetPitstopShopsUseCase().execute(new GetPitstopShopsUseCase.Callback() {
            @Override
            public void onShopsGot(List<Dealership> dealerships) {
                if(view != null){
                    List<Dealership> sortedDealers = sortShops(dealerships);
                    view.showDealershipList(sortedDealers);
                    localDealerships = sortedDealers;
                    view.loading(false);
                }
            }

            @Override
            public void onError(RequestError error) {
                if(view != null){
                    view.loading(false);
                    view.toast(((Fragment) view).getString(R.string.error_loading_pitstop_shops_toast_message));
                }
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
        Log.d(TAG,"changeShop() dealership: "+dealership);
        if(view == null){return;}
        Car car = view.getCar();

        component.getUpdateCarDealershipUseCase().execute(car.getId(), dealership
                , EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
            @Override
            public void onCarDealerUpdated() {
                Log.d(TAG,"onCarDealerUpdated() view: "+ view);
                if(view != null){
                    switcher.endCustomShops(dealership.getName());
                }
            }

            @Override
            public void onError(RequestError error) {
                view.toast(((Fragment) view).getString(R.string.error_selecting_shop_toast_message));

            }
        });
    }

    public void filterShops(String filter){
        if(view == null){return;}
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
        view.showDealershipList(dealerships);
    }

    @Override
    public void onShopClicked(Dealership dealership) {
        if(view == null){return;}
        mixpanelHelper.trackButtonTapped("PitstopShop","PitstopShops");
        view.showConfirmation(dealership);
    }
}

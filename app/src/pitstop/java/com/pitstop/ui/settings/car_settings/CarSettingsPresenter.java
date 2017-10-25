package com.pitstop.ui.settings.car_settings;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.set.SetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matthew on 2017-06-13.
 */

public class CarSettingsPresenter {
    private final String CHANGE_SHOP = "pref_change_shop";
    private final String DELETE_KEY = "pre_delete_car";
    private final String SET_CURRENT_KEY = "pref_set_active";

    private CarSettingsView carSettings;
    private FragmentSwitcher switcher;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;




    public CarSettingsPresenter(FragmentSwitcher switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }


    public void subscribe(CarSettingsView carSettings){
        mixpanelHelper.trackViewAppeared("CarSettings");
        this.carSettings = carSettings;
    }

    public void unsubscribe(){
        this.carSettings = null;
    }

    public void preferenceClicked(String key){
        if(carSettings == null || switcher == null){return;}
        if(key.equals(CHANGE_SHOP)){
            mixpanelHelper.trackButtonTapped("ChangeShop","CarSettings");
            switcher.startCustomShops(carSettings.getCar());
        }else if(key.equals(DELETE_KEY)){
            mixpanelHelper.trackButtonTapped("DeleteCar","CarSettings");
            carSettings.showDelete();
        }else if(key.equals(SET_CURRENT_KEY)){
            mixpanelHelper.trackButtonTapped("SetAsCurrent","CarSettings");
            component.setUseCarUseCase().execute(carSettings.getCar().getId(), EventSource.SOURCE_SETTINGS, new SetUserCarUseCase.Callback() {
                @Override
                public void onUserCarSet() {
                    if(carSettings != null && switcher != null){
                        switcher.setViewMainSettings();
                    }
                }

                @Override
                public void onError(RequestError error) {
                    if(carSettings != null){
                        carSettings.toast("An error occurred while updating your car");
                    }
                }
            });


        }
    }
    void updateCar(int carId){
        if(carSettings == null){return;}
        component.getGetCarByCarIdUseCase().execute(carId, new GetCarByCarIdUseCase.Callback() {
            @Override
            public void onCarGot(Car car, Dealership dealership) {
                if(carSettings != null){
                    carSettings.setData(car, dealership);
                    if(dealership != null) {
                        carSettings.showCarText(car.getMake() + " " + car.getModel(), dealership.getName());
                    }
                }
            }

            @Override
            public void onError(RequestError error) {
                if(carSettings != null){
                    carSettings.toast("There was an error loading your car details");
                }
            }
        });


    }

    void deleteCar(Car car){
        if(carSettings == null){return;}
        component.removeCarUseCase().execute(car.getId(),EventSource.SOURCE_SETTINGS, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {
                if(carSettings != null && switcher != null){
                    switcher.setViewMainSettings();
                }
            }
            @Override
            public void onError(RequestError error) {
                if(carSettings != null){
                    carSettings.toast("There was an error removing your car");
                }
            }
        });
    }
}

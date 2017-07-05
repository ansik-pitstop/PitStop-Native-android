package com.pitstop.ui.settings.car_settings;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetCarByCarIdUseCase;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.ui.settings.FragmentSwitcher;



/**
 * Created by Matthew on 2017-06-13.
 */

public class CarSettingsPresenter {
    private final String CHANGE_SHOP = "pref_change_shop";
    private final String DELETE_KEY = "pre_delete_car";
    private final String SET_CURRENT_KEY = "pref_set_active";

    private CarSettingsInterface carSettings;
    private FragmentSwitcher switcher;
    private UseCaseComponent component;




    public CarSettingsPresenter(FragmentSwitcher switcher, UseCaseComponent component){
        this.switcher = switcher;
        this.component = component;
    }


    public void subscribe(CarSettingsInterface carSettings){
        this.carSettings = carSettings;
    }
    public void preferenceClicked(String key){
        if(key.equals(CHANGE_SHOP)){
            switcher.startCustomShops(carSettings.getCar());
        }else if(key.equals(DELETE_KEY)){
            carSettings.showDelete();
        }else if(key.equals(SET_CURRENT_KEY)){
            component.setUseCarUseCase().execute(carSettings.getCar().getId(), new SetUserCarUseCase.Callback() {
                @Override
                public void onUserCarSet() {
                    switcher.setViewMainSettings();
                }

                @Override
                public void onError() {
                    carSettings.toast("An error occurred while updating your car");
                }
            });


        }
    }
    void updateCar(int carId){
        component.getGetCarByCarIdUseCase().execute(carId, new GetCarByCarIdUseCase.Callback() {
            @Override
            public void onCarGot(Car car) {
                carSettings.setCar(car);
                if(car.getDealership() != null) {
                    carSettings.showCarText(car.getMake() + " " + car.getModel(), car.getDealership().getName());
                }
            }

            @Override
            public void onError() {
                carSettings.toast("There was an error loading your car details");

            }
        });


    }

    void deleteCar(Car car){
        component.removeCarUseCase().execute(car, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {
                switcher.setViewMainSettings();
            }
            @Override
            public void onError() {
                carSettings.toast("There was an error removing your car");

            }
        });
    }
}

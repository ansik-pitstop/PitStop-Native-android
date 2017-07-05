package com.pitstop.ui.settings.main_settings;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCurrentUserUseCase;
import com.pitstop.interactors.GetUserShopsUseCase;
import com.pitstop.interactors.UpdateUserNameUseCase;
import com.pitstop.interactors.UpdateUserPhoneUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;

import java.util.Collections;
import java.util.List;


/**
 * Created by Matt on 2017-06-12.
 */

public class MainSettingsPresenter {
    private final String NAME_PREF_KEY = "pref_username_key";
    private final String PHONE_PREF_KEY = "pref_phone_number_key";
    private final String ADD_CAR_KEY = "add_car_button";
    private final String PRIV_KEY = "pref_privacy_policy";
    private  final String TERMS_KEY = "pref_term_of_use";
    private final String LOG_OUT_KEY = "pref_logout";

    private MainSettingsInterface mainSettings;
    private FragmentSwitcher switcher;
    private PrefMaker prefMaker;
    private UseCaseComponent component;

    private int processesFinished;


    public MainSettingsPresenter(FragmentSwitcher switcher, PrefMaker prefMaker, UseCaseComponent component){
        this.switcher = switcher;
        this.prefMaker = prefMaker;
        this.component = component;

    }

    void subscribe(MainSettingsInterface mainSettings ){
        this.mainSettings = mainSettings;
        switcher.loading(true);
    }
    void setVersion(){
        mainSettings.showVersion(mainSettings.getBuildNumber());
    }

    void preferenceClicked(String prefKey){
        if(prefKey.equals(ADD_CAR_KEY)){
            switcher.startAddCar();
        }else if(prefKey.equals(PRIV_KEY)){
            mainSettings.startPriv();
        }else if(prefKey.equals(TERMS_KEY)){
            mainSettings.startTerms();
        }else if(prefKey.equals(LOG_OUT_KEY)){
            mainSettings.showLogOut();
        }
    }

    public void update(){
        switcher.loading(true);
        processesFinished = 0;
        getUser();
        getCars();
        getShops();
    }


    public void getCars(){// this needs to be changed
        component.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback(){
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                mainSettings.resetCars();
                Collections.reverse(cars);
                for(Car c:cars) {
                    mainSettings.addCar(prefMaker.carToPref(c,c.isCurrentCar()));
                }
                checkDone();
            }
            @Override
            public void onError() {
                mainSettings.toast("There was an error loading your cars");
                checkDone();
            }
        });
    }
    public void getUser(){
        component.getGetCurrentUserUseCase().execute(new GetCurrentUserUseCase.Callback() {
            @Override
            public void onUserRetrieved(User user) {
                String username = user.getFirstName() + " " + user.getLastName();
                String phone = user.getPhone();
                mainSettings.showName(username);
                mainSettings.showPhone(phone);
                mainSettings.showEmail(user.getEmail());
                mainSettings.setPrefs(username,phone);
                checkDone();
            }

            @Override
            public void onError() {
                mainSettings.toast("There was an error loading your details");
                checkDone();
            }
        });
    }

    public void getShops(){
        component.getGetUserShopsUseCase().execute(new GetUserShopsUseCase.Callback() {
            @Override
            public void onShopGot(List<Dealership> dealerships) {
                mainSettings.resetShops();
                for(Dealership d : dealerships){
                    mainSettings.addShop(prefMaker.shopToPref(d));
                }
                checkDone();
            }

            @Override
            public void onError() {
                mainSettings.toast("There was an error loading your shops");
                checkDone();
            }
        });

    }

    private void checkDone(){
        processesFinished++;
        if(processesFinished == 3){
            switcher.loading(false);
        }
    }

    public void preferenceInput(String text, String key){
        if(key.equals(NAME_PREF_KEY)){
            mainSettings.showName(text);
            component.getUpdateUserNameUseCase().execute(text, new UpdateUserNameUseCase.Callback() {
                @Override
                public void onUserNameUpdated() {
                }

                @Override
                public void onError() {
                }
            });
        }else if(key.equals(PHONE_PREF_KEY)){
            mainSettings.showPhone(text);
            component.getUpdateUserPhoneUseCase().execute(text, new UpdateUserPhoneUseCase.Callback() {
                @Override
                public void onUserPhoneUpdated() {

                }

                @Override
                public void onError() {

                }
            });

        }
    }
    public void logout(){
        mainSettings.logout();
        mainSettings.gotoLogin();
    }
}

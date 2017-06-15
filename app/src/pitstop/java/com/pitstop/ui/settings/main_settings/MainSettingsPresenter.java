package com.pitstop.ui.settings.main_settings;

import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCurrentUserUseCase;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.UserRepository;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;

import java.util.List;

import javax.inject.Inject;

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


    @Inject
    GetCarsByUserIdUseCase getCarsByUserIdUseCase;

    @Inject
    GetCurrentUserUseCase getCurrentUserUseCase;

    @Inject
    GetUserCarUseCase getUserCarUseCase;

    void subscribe(MainSettingsInterface mainSettings, FragmentSwitcher switcher, PrefMaker prefMaker){
        this.mainSettings = mainSettings;
        this.switcher = switcher;
        this.prefMaker = prefMaker;
    }
    void setVersion(){
        mainSettings.showVersion(mainSettings.getBuildNumber());
    }

    void preferenceClicked(String prefKey){
        if(prefKey.equals(ADD_CAR_KEY)){
            mainSettings.startAddCar();
        }else if(prefKey.equals(PRIV_KEY)){
            mainSettings.startPriv();
        }else if(prefKey.equals(TERMS_KEY)){
            mainSettings.startTerms();
        }else if(prefKey.equals(LOG_OUT_KEY)){
            mainSettings.showLogOut();
        }
    }
    public void getCars(){
        getUserCarUseCase.execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                getCarsByUserIdUseCase.execute(new GetCarsByUserIdUseCase.Callback(){
                    @Override
                    public void onCarsRetrieved(List<Car> cars) {
                        mainSettings.resetCars();
                        for(Car c:cars) {
                            mainSettings.addCar(prefMaker.carToPref(c, (c.getId() == car.getId())));
                        }
                        System.out.println("Testing "+ cars);
                    }
                    @Override
                    public void onError() {
                        System.out.println("Testing error");
                    }
                });
            }

            @Override
            public void onError() {

            }
        });

    }
    public void getUser(){
        getCurrentUserUseCase.execute(new GetCurrentUserUseCase.Callback() {
            @Override
            public void onUserRetrieved(User user) {
                mainSettings.showName(user.getFirstName() + " " + user.getLastName());
                mainSettings.showPhone(formatPhone(user.getPhone()));
                mainSettings.showEmail(user.getEmail());
            }

            @Override
            public void onError() {

            }
        });
    }
    private String formatPhone(String phone){// might be a bad idea
        return "(" + phone.substring(0,3) + ") " + phone.substring(3,6) + "-" + phone.substring(6);
    }

    public void preferenceInput(String text, String key){
        if(key.equals(NAME_PREF_KEY)){
            mainSettings.showName(text);
        }else if(key.equals(PHONE_PREF_KEY)){
            mainSettings.showPhone(text);
        }
    }
    public void logout(){
        mainSettings.logout();
        mainSettings.gotoLogin();
    }
}

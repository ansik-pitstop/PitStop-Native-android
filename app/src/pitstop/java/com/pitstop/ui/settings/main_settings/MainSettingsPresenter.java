package com.pitstop.ui.settings.main_settings;

import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.ContextRelated;

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
    private ContextRelated launcher;

    void subscribe(MainSettingsInterface mainSettings, FragmentSwitcher switcher, ContextRelated launcher){
        this.mainSettings = mainSettings;
        this.switcher = switcher;
        this.launcher = launcher;

    }
    void setVersion(){
        mainSettings.showVersion(launcher.getBuildNumber());
    }

    void preferenceClicked(String prefKey){
        if(prefKey.equals(ADD_CAR_KEY)){
            launcher.startAddCar();
        }else if(prefKey.equals(PRIV_KEY)){
            launcher.startPriv();
        }else if(prefKey.equals(TERMS_KEY)){
            launcher.startTerms();
        }else if(prefKey.equals(LOG_OUT_KEY)){
            launcher.showLogOut();
        }
    }

    void preferenceInput(String text, String key){
        if(key.equals(NAME_PREF_KEY)){
            mainSettings.showName(text);
        }else if(key.equals(PHONE_PREF_KEY)){
            mainSettings.showPhone(text);
        }
    }
    void addCar(){

    }

    void setupPrefs(){
        mainSettings.showVersion(launcher.getBuildNumber());

    }
}

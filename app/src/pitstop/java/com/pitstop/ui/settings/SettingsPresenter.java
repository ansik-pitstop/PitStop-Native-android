package com.pitstop.ui.settings;

import android.content.Intent;

import com.pitstop.models.Car;

import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsPresenter {
    private SettingsInterface settings;
    private FragmentSwitcher switcher;


    public void subscribe(SettingsInterface settings, FragmentSwitcher switcher){
        this.settings = settings;
        this.switcher = switcher;
    }
    public void setViewMainSettings(){
        switcher.setViewMainSettings();
    }



    public void setViewCarSettings(){
        switcher.setViewCarSettings();
    }

    public void setViewShopSettings(){
        switcher.setViewShopSettings();
    }


}

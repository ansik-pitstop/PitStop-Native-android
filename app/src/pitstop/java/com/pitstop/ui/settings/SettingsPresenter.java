package com.pitstop.ui.settings;


/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsPresenter {
    private SettingsView settings;
    private FragmentSwitcher switcher;

    public SettingsPresenter( FragmentSwitcher switcher){
        this.switcher = switcher;
    }

    public void subscribe(SettingsView settings){
        this.settings = settings;
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

package com.pitstop.ui.settings.main_settings;


import android.preference.Preference;

import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;

/**
 * Created by Matt on 2017-06-12.
 */

public interface MainSettingsInterface {
    void setSwitcher(FragmentSwitcher switcher);
    void startAddCar();
    void startPriv();
    void resetCars();
    void startTerms();
    void showLogOut();
    void gotoLogin();
    void setPrefMaker(PrefMaker prefMaker);
    void addCar(Preference preference);
    void logout();
    void showName(String name);
    void showEmail(String email);
    void showPhone(String phone);
    void showVersion(String version);
    String getBuildNumber();
}

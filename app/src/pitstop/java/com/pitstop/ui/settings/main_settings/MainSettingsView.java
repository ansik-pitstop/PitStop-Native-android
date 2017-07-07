package com.pitstop.ui.settings.main_settings;


import android.preference.Preference;

import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;

/**
 * Created by Matt on 2017-06-12.
 */

public interface MainSettingsView {
    void setSwitcher(FragmentSwitcher switcher);
    void startPriv();
    void resetCars();
    void resetShops();
    void startTerms();
    void showLogOut();
    void gotoLogin();
    void setPrefs(String name, String phone);
    void setPrefMaker(PrefMaker prefMaker);
    void addCar(Preference preference);
    void addShop(Preference preference);
    void logout();
    void showName(String name);
    void showEmail(String email);
    void showPhone(String phone);
    void showVersion(String version);
    String getBuildNumber();
    void toast(String message);
}

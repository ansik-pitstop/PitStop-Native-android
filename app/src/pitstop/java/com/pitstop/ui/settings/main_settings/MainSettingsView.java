package com.pitstop.ui.settings.main_settings;


import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by Matt on 2017-06-12.
 */

public interface MainSettingsView {
    void setSwitcher(FragmentSwitcher switcher);
    void gotoLogin();
    void setPrefs(String name, String phone);
    void logout();
    void showName(String name);
    void showEmail(String email);
    void showPhone(String phone);
    void showVersion(String version);
    String getBuildNumber();
    void toast(String message);
    void startPriv();
    void startTerms();
    void showLogOut();
}

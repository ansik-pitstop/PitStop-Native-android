package com.pitstop.ui.settings.main_settings;


import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.ContextRelated;

/**
 * Created by Matt on 2017-06-12.
 */

public interface MainSettingsInterface {
    void setSwitcher(FragmentSwitcher switcher);
    void setLauncher(ContextRelated launcher);
    void showName(String name);
    void showPhone(String phone);
    void showVersion(String version);
}

package com.pitstop.ui.settings;

/**
 * Created by Matt on 2017-06-12.
 */

public interface FragmentSwitcher {
    void setViewMainSettings();
    void setViewCarSettings();
    void loading(boolean show);
    void startAddCar();
}

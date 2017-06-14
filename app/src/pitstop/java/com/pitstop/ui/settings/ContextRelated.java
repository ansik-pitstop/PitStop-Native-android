package com.pitstop.ui.settings;

import android.preference.Preference;

import com.pitstop.models.Car;

/**
 * Created by xirax on 2017-06-13.
 */

public interface ContextRelated {// Intents dialogs and stuff
    void startPriv();
    void startTerms();
    void startAddCar();
    void showLogOut();
    String getBuildNumber();
    Preference carToPref(Car car);
}

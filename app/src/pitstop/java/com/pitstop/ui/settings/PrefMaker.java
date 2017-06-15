package com.pitstop.ui.settings;

import android.preference.Preference;

import com.pitstop.models.Car;

/**
 * Created by xirax on 2017-06-14.
 */

public interface PrefMaker {
    Preference carToPref(Car car, boolean currentCar);
}

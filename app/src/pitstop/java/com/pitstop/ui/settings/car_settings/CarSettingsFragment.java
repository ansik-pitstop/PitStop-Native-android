package com.pitstop.ui.settings.car_settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.models.Car;

/**
 * Created by xirax on 2017-06-13.
 */

public class CarSettingsFragment extends PreferenceFragment implements CarSettingsInterface {
    private final String CAR_TITLE = "pref_car_tile";
    private final String CHANGE_SHOP = "pref_change_shop";

    private PreferenceCategory carCatagory;
    private Preference changeDealer;

    private Car car;

    @Override
    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        addPreferencesFromResource(R.xml.car_preferences);
        carCatagory = (PreferenceCategory) findPreference(CAR_TITLE);
        changeDealer = (Preference) findPreference(CHANGE_SHOP);
        carCatagory.setTitle(car.getMake()+" "+car.getModel());
        changeDealer.setTitle(car.getDealership().getName());
        return view;
    }
}

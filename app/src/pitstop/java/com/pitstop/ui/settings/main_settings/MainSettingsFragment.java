package com.pitstop.ui.settings.main_settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.ContextRelated;


/**
 * Created by Matt on 2017-06-12.
 */

public class MainSettingsFragment extends PreferenceFragment implements MainSettingsInterface, SharedPreferences.OnSharedPreferenceChangeListener  {
    private final String NAME_PREF_KEY = "pref_username_key";
    private final String PHONE_PREF_KEY = "pref_phone_number_key";
    private final String APP_INFO_KEY = "AppInfo";

    private MainSettingsPresenter presenter;
    private FragmentSwitcher switcher;
    private ContextRelated launcher;

    private Preference infoPreference;
    private EditTextPreference namePreference;
    private EditTextPreference phonePreference;
    private PreferenceCategory vehicleCatagory;

    @Override
    public void setSwitcher(FragmentSwitcher switcher) {
        this.switcher = switcher;
    }

    @Override
    public void setLauncher(ContextRelated launcher) {
        this.launcher = launcher;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        namePreference = (EditTextPreference) findPreference(NAME_PREF_KEY);
        phonePreference = (EditTextPreference) findPreference(PHONE_PREF_KEY);
        infoPreference = (Preference) findPreference(APP_INFO_KEY);
        vehicleCatagory = (PreferenceCategory) findPreference(getString(R.string.pref_vehicles));

        presenter = new MainSettingsPresenter();
        presenter.subscribe(this,switcher,launcher);
        presenter.setVersion();

        //presenter.setupPrefs();
        return view;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        System.out.println("Testing "+preference.getKey());
        presenter.preferenceClicked(preference.getKey());
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void showName(String name) {
        namePreference.setTitle(name);
    }

    @Override
    public void showPhone(String phone) {
        phonePreference.setTitle(phone);
    }

    @Override
    public void showVersion(String version) {
        infoPreference.setTitle(version);
    }


    public void addCar(Preference carPref) {
        vehicleCatagory.addPreference(carPref);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(NAME_PREF_KEY) || key.equals(PHONE_PREF_KEY)){
            presenter.preferenceInput(sharedPreferences.getString(key,""),key);
        }
    }


}


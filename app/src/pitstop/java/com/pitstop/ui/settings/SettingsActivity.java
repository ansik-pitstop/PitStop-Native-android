package com.pitstop.ui.settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.settings.car_settings.CarSettingsFragment;
import com.pitstop.ui.settings.main_settings.MainSettingsFragment;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsActivity extends AppCompatActivity implements SettingsInterface,FragmentSwitcher,PrefMaker {
    private SettingsPresenter presenter;
    private FragmentManager fragmentManager;
    private MainSettingsFragment mainSettings;
    private CarSettingsFragment carSettings;
    private Context context;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_settings);
        fragmentManager = getFragmentManager();
        mainSettings = new MainSettingsFragment();
        carSettings = new CarSettingsFragment();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);//will probably need to move these to the activity
        sharedPrefs.registerOnSharedPreferenceChangeListener(mainSettings);
        mainSettings.setSwitcher(this);
        mainSettings.setPrefMaker(this);

        presenter = new SettingsPresenter();
        presenter.subscribe(this,this);
        presenter.setViewMainSettings();
    }

    @Override
    public void setViewMainSettings() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_holder,mainSettings);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewCarSettings() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_holder,carSettings);
        fragmentTransaction.addToBackStack("car_settings");
        fragmentTransaction.commit();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            if(requestCode == RC_ADD_CAR){
                if (resultCode == AddCarActivity.ADD_CAR_SUCCESS || resultCode == AddCarActivity.ADD_CAR_NO_DEALER_SUCCESS) { // probably wrong but dont know how to do otherwise
                    presenter.carAdded(data);
                }
            }
        }
    }

    @Override
    public Preference carToPref(Car car){// this is a tough one to decouple
        Preference carPref = new Preference(context);
        if(car.isCurrentCar()){
            carPref.setIcon(R.drawable.ic_check_circle_green_400_36dp);
            System.out.println("Testing isCurrent "+car);
        }
        carPref.setTitle(car.getMake() + " " +car.getModel());
        carPref.setSummary(car.getDealership().getName());
        carPref.setKey("car_item");
        carPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                carSettings.setCar(car);
                presenter.setViewCarSettings();
                return false;
            }
        });
        return carPref;
    }







}

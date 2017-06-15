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
import android.view.MenuItem;

import com.pitstop.R;
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
        carSettings.setSwitcher(this);
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
    public Preference carToPref(Car car, boolean currentCar){// this is a tough one to decouple
        Preference carPref = new Preference(context);
        if(currentCar){
            carPref.setWidgetLayoutResource(R.layout.vehicle_pref_icon);
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

    @Override
    public void onBackPressed() {
       if(carSettings.isVisible()){
           presenter.setViewMainSettings();
       }else{
         finish();
       }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
          this.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}

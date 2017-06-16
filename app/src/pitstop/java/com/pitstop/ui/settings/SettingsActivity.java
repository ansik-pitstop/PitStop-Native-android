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
import android.view.View;
import android.widget.ProgressBar;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.settings.car_settings.CarSettingsFragment;
import com.pitstop.ui.settings.main_settings.MainSettingsFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

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


    @BindView(R.id.settings_progress)
    ProgressBar loadingSpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        fragmentManager = getFragmentManager();
        mainSettings = new MainSettingsFragment();
        carSettings = new CarSettingsFragment();
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
    public void startAddCar() {//onActivityResult doesn't work if I do this in the fragment
        Intent intent = new Intent(this,AddCarActivity.class);
        startActivityForResult(intent,RC_ADD_CAR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Check for Add car finished, and whether it happened successfully, if so updated preferences view
        if (data != null) {
            if (requestCode == RC_ADD_CAR) {
                if (resultCode == AddCarActivity.ADD_CAR_SUCCESS || resultCode == AddCarActivity.ADD_CAR_NO_DEALER_SUCCESS) {
                    mainSettings.update();//This is where the hack gets called
                    //might be a good place to use the event bus
                }
            }
        }
    }


    @Override
    public void loading(boolean show) {
        if(show){
            loadingSpinner.setVisibility(View.VISIBLE);
        }else{
            loadingSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public Preference carToPref(Car car, boolean currentCar){// this is a tough one to decouple
        Preference carPref = new Preference(context);
        if(currentCar){
            carPref.setWidgetLayoutResource(R.layout.vehicle_pref_icon);
        }
        carPref.setTitle(car.getMake() + " " +car.getModel());
        carPref.setSummary("Tap to manage");
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
}

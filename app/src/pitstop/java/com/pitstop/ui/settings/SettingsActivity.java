package com.pitstop.ui.settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.custom_shops.CustomShopActivity;
import com.pitstop.ui.custom_shops.view_fragments.ShopForm.ShopFormFragment;
import com.pitstop.ui.settings.car_settings.CarSettingsFragment;
import com.pitstop.ui.settings.main_settings.MainSettingsFragment;
import com.pitstop.ui.settings.shop_settings.ShopSettingsFragment;


import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;
import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsActivity extends AppCompatActivity implements SettingsInterface,FragmentSwitcher, PrefMaker {
    private SettingsPresenter presenter;
    private FragmentManager fragmentManager;
    private MainSettingsFragment mainSettings;
    private CarSettingsFragment carSettings;
    private ShopSettingsFragment shopSettings;
    private ShopFormFragment shopForm;
    private Context context;

    private final int START_CUSTOM = 347;

    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);


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
        shopSettings = new ShopSettingsFragment();
        shopForm = new ShopFormFragment();

        mainSettings.setSwitcher(this);
        carSettings.setSwitcher(this);
        mainSettings.setPrefMaker(this);
        shopSettings.setSwitcher(this);
        shopForm.setSwitcher(this);
        shopForm.setUpdate(true);
        presenter = new SettingsPresenter();
        presenter.subscribe(this,this);
        presenter.setViewMainSettings();
    }
    @Override
    public void setViewShopForm(Dealership dealership){//here
        shopForm.setDealership(dealership);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_holder,shopForm);
        fragmentTransaction.commit();
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
    public void setViewShopSettings() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_holder,shopSettings);
        fragmentTransaction.commit();
    }

    @Override
    public void startAddCar() {//onActivityResult doesn't work if I do this in the fragment
        Intent intent = new Intent(this,AddCarActivity.class);
        startActivityForResult(intent,RC_ADD_CAR);
    }

    @Override
    public void startCustomShops(Car car) {
        Intent intent = new Intent(context, CustomShopActivity.class);
        intent.putExtra(CAR_EXTRA,car);
        startActivityForResult(intent,START_CUSTOM);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Check for Add car finished, and whether it happened successfully, if so updated preferences view
        mainSettings.update();
        carSettings.update();
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
    public Preference shopToPref(Dealership dealership) {
        Preference shopPref = new Preference(context);
        shopPref.setTitle(dealership.getName());
        shopPref.setKey("shop_item");
        shopPref.setSummary(dealership.getAddress());
        shopPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                shopSettings.setDealership(dealership);
                presenter.setViewShopSettings();
                return false;
            }
        });
        return shopPref;
    }

    @Override
    public void onBackPressed() {
        if(shopForm.isVisible()){
            presenter.setViewShopSettings();
        }
        else if(carSettings.isVisible()||shopSettings.isVisible()){
           presenter.setViewMainSettings();
       }else{
           finish();
       }
    }

    /*@Override
    public void finish() {
        super.finish();
        EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP);
        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                ,EVENT_SOURCE));
        EventType eventType2 = new EventTypeImpl(EventType.EVENT_CAR_ID);
        EventBus.getDefault().post(new CarDataChangedEvent(eventType2
                ,EVENT_SOURCE));
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(shopForm.isVisible()){
            presenter.setViewShopSettings();
        }
        else if(carSettings.isVisible()||shopSettings.isVisible()){
            presenter.setViewMainSettings();
        }else{
            finish();
        }
        return true;
    }
}

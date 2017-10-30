package com.pitstop.ui.settings.car_settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matthew on 2017-06-13.
 */

public class CarSettingsFragment extends PreferenceFragment implements CarSettingsView {
    private final String CAR_TITLE = "pref_car_tile";
    private final String CHANGE_SHOP = "pref_change_shop";
    private final String DELETE_KEY = "pre_delete_car";
    private final String SET_CURRENT_KEY = "pref_set_active";

    private GlobalApplication application;
    private Context context;

    private FragmentSwitcher switcher;

    private PreferenceCategory carCatagory;
    private Preference changeDealer;
    private CarSettingsPresenter presenter;

    private Car car;
    private Dealership dealership;

    private MixpanelHelper mixpanelHelper;

    @Override
    public void setSwitcher(FragmentSwitcher switcher) {
        this.switcher = switcher;
    }

    @Override
    public void setData(Car car, Dealership dealership) {
        this.car = car;
        this.dealership = dealership;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;


        addPreferencesFromResource(R.xml.car_preferences);

        View view = super.onCreateView(inflater, container, savedInstanceState);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new CarSettingsPresenter(switcher,component,mixpanelHelper);
        presenter.subscribe(this);



        carCatagory = (PreferenceCategory) findPreference(CAR_TITLE);
        changeDealer = (Preference) findPreference(CHANGE_SHOP);
        if(car != null){
            carCatagory.setTitle(car.getMake()+" "+car.getModel());
            if(dealership == null){
                changeDealer.setTitle("No Dealership");
            }else{
                changeDealer.setTitle(dealership.getName());
            }
        }
        return view;
    }

    @Override
    public void showCarText(String name, String shop) {
        carCatagory.setTitle(name);
        changeDealer.setTitle(shop);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void showDelete() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.delete)+ car.getMake() +" "+ car.getModel());
        alertDialogBuilder
                .setMessage(getString(R.string.delete_car))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                        presenter.deleteCar(car);
                    }
                })
                .setNegativeButton(getString(R.string.no_button_text),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void update() {
        if(car == null){
            return;
        }
        presenter.updateCar(car.getId());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        presenter.preferenceClicked(preference.getKey());
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}

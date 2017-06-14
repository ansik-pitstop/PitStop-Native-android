package com.pitstop.ui.settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.application.LogOutable;
import com.pitstop.models.Car;
import com.pitstop.ui.LoginActivity;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.settings.car_settings.CarSettingsFragment;
import com.pitstop.ui.settings.main_settings.MainSettingsFragment;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;
import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsActivity extends AppCompatActivity implements SettingsInterface,FragmentSwitcher,ContextRelated {
    private SettingsPresenter presenter;
    private FragmentManager fragmentManager;
    private MainSettingsFragment mainSettings;
    private CarSettingsFragment carSettings;
    private LogOutable logOutable;
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
        logOutable = (GlobalApplication)context;
        mainSettings.setSwitcher(this);
        mainSettings.setLauncher(this);
        presenter = new SettingsPresenter();
        presenter.subscribe(this,this,logOutable,getApplicationContext());
        presenter.setViewMainSettings();
        presenter.getCars();
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
    public void startAddCar() {
        Intent intent = new Intent(this, AddCarActivity.class);
        startActivityForResult(intent,RC_ADD_CAR);
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
    public void startPriv() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/PrivacyPolicy.pdf")));
    }

    @Override
    public void startTerms() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/AppAgreement.pdf")));
    }
    @Override
    public void showLogOut() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);//will probably need to move these to the activity
        alertDialogBuilder.setTitle("Log Out");
        alertDialogBuilder
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                        presenter.logout();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void gotoLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    @Override
    public String getBuildNumber() {
        try{
           return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return "";
    }
}

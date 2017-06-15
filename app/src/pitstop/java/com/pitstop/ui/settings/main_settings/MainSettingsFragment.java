package com.pitstop.ui.settings.main_settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.LoginActivity;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;


/**
 * Created by Matt on 2017-06-12.
 */

public class MainSettingsFragment extends PreferenceFragment implements MainSettingsInterface, SharedPreferences.OnSharedPreferenceChangeListener  {
    private final String NAME_PREF_KEY = "pref_username_key";
    private final String PHONE_PREF_KEY = "pref_phone_number_key";
    private final String APP_INFO_KEY = "AppInfo";
    private final String EMAIL_PREF_KEY = "pref_email_key";

    private GlobalApplication application;
    private Context context;

    private MainSettingsPresenter presenter;
    private FragmentSwitcher switcher;
    private PrefMaker prefMaker;

    private Preference infoPreference;
    private Preference emailPreference;
    private EditTextPreference namePreference;
    private EditTextPreference phonePreference;
    private PreferenceCategory vehicleCatagory;
    private boolean prefsCreated = false;

    @Override
    public void setSwitcher(FragmentSwitcher switcher) {
        this.switcher = switcher;
    }

    @Override
    public void setPrefMaker(PrefMaker prefMaker) {
        this.prefMaker = prefMaker;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;


        addPreferencesFromResource(R.xml.preferences);

        View view = super.onCreateView(inflater, container, savedInstanceState);
        namePreference = (EditTextPreference) findPreference(NAME_PREF_KEY);
        phonePreference = (EditTextPreference) findPreference(PHONE_PREF_KEY);
        infoPreference = (Preference) findPreference(APP_INFO_KEY);
        emailPreference = (Preference) findPreference(EMAIL_PREF_KEY);
        vehicleCatagory = (PreferenceCategory) findPreference(getString(R.string.pref_vehicles));

        presenter = new MainSettingsPresenter();
        presenter.subscribe(this,switcher,prefMaker);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(presenter);

        presenter.setVersion();
        presenter.getCars();
        presenter.getUser();
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
    public void showEmail(String email) {
        emailPreference.setTitle(email);
    }

    @Override
    public void showPhone(String phone) {
        phonePreference.setTitle(phone);
    }

    @Override
    public void showVersion(String version) {
        infoPreference.setTitle(version);
    }

    @Override
    public void addCar(Preference preference) {
        vehicleCatagory.addPreference(preference);
    }

    @Override
    public void resetCars() {
        vehicleCatagory.removeAll();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(NAME_PREF_KEY) || key.equals(PHONE_PREF_KEY)){
            presenter.preferenceInput(sharedPreferences.getString(key,""),key);
        }
    }
    @Override
    public void startAddCar() {
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent,RC_ADD_CAR);
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
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
    public void logout(){
        application.logOutUser();
    }

    @Override
    public void gotoLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}


package com.pitstop.ui.settings.main_settings;

import android.content.ActivityNotFoundException;
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
import android.preference.PreferenceManager;
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
import com.pitstop.ui.LoginActivity;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.ui.settings.PrefMaker;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matt on 2017-06-12.
 */

public class MainSettingsFragment extends PreferenceFragment implements MainSettingsView, SharedPreferences.OnSharedPreferenceChangeListener  {
    private final String NAME_PREF_KEY = "pref_username_key";
    private final String PHONE_PREF_KEY = "pref_phone_number_key";
    private final String APP_INFO_KEY = "AppInfo";
    private final String EMAIL_PREF_KEY = "pref_email_key";
    private final String SHOP_PREF_KEY = "pref_shops";

    private GlobalApplication application;
    private Context context;

    private MainSettingsPresenter presenter;
    private FragmentSwitcher switcher;
    private PrefMaker prefMaker;

    private SharedPreferences sharedPrefs;

    private Preference infoPreference;
    private Preference emailPreference;
    private EditTextPreference namePreference;
    private EditTextPreference phonePreference;
    private PreferenceCategory vehicleCatagory;
    private PreferenceCategory shopCatagory;

    private MixpanelHelper mixpanelHelper;

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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        namePreference = (EditTextPreference) findPreference(NAME_PREF_KEY);
        phonePreference = (EditTextPreference) findPreference(PHONE_PREF_KEY);
        infoPreference = (Preference) findPreference(APP_INFO_KEY);
        emailPreference = (Preference) findPreference(EMAIL_PREF_KEY);
        vehicleCatagory = (PreferenceCategory) findPreference(getString(R.string.pref_vehicles));
        shopCatagory = (PreferenceCategory) findPreference(SHOP_PREF_KEY);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new MainSettingsPresenter(switcher,prefMaker,component,mixpanelHelper);
        presenter.subscribe(this);



        presenter.setVersion();
        presenter.update();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.update();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setPrefs(String name, String phone){//these are the same as the last user so they need to be updated here
        sharedPrefs.edit().putString(NAME_PREF_KEY,name).commit();
        sharedPrefs.edit().putString(PHONE_PREF_KEY,phone).commit();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        presenter.preferenceClicked(preference.getKey());
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    public void update(){//this is a hack
        presenter.update();
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
    public void addShop(Preference preference) {
        shopCatagory.addPreference(preference);
    }

    @Override
    public void resetCars() {
        vehicleCatagory.removeAll();
    }

    @Override
    public void resetShops() {
        shopCatagory.removeAll();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(NAME_PREF_KEY) || key.equals(PHONE_PREF_KEY)){
            presenter.preferenceInput(sharedPreferences.getString(key,""),key);
        }
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
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pitstopconnect.com/privacypolicy/PrivacyPolicy.pdf")));
    }

    @Override
    public void startTerms() {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pitstopconnect.com/privacypolicy/AppAgreement.pdf")));
    }
    @Override
    public void showLogOut() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.log_out));
        alertDialogBuilder
                .setMessage(getString(R.string.log_out_confirm_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                        presenter.logout();
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


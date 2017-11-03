package com.pitstop.ui.settings.shop_settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
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
import com.pitstop.models.Dealership;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matthew on 2017-06-26.
 */

public class ShopSettingsFragment extends PreferenceFragment implements ShopSettingsView {
    private final String SHOP_TITLE_KEY = "pref_Shop_title";
    private final String EDIT_SHOP_KEY = "pref_edit_shop";
    private final String DELETE_SHOP_KEY = "pref_delete_shop";

    private FragmentSwitcher switcher;

    private Dealership dealership;

    private PreferenceCategory shopTitle;
    private Preference editButton;
    private Preference deleteButton;

    private ShopSettingsPresenter presenter;

    private Context context;
    private GlobalApplication application;

    private MixpanelHelper mixpanelHelper;


    public void setSwitcher(FragmentSwitcher switcher){
        this.switcher = switcher;
    }

    public void setDealership(Dealership dealership){
        this.dealership = dealership;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = super.onCreateView(inflater, container, savedInstanceState);
        addPreferencesFromResource(R.xml.shop_preference);
        shopTitle = (PreferenceCategory) findPreference(SHOP_TITLE_KEY);
        editButton = findPreference(EDIT_SHOP_KEY);
        deleteButton = findPreference(DELETE_SHOP_KEY);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new ShopSettingsPresenter(switcher,component,mixpanelHelper);
        presenter.subscribe(this);



        shopTitle.setTitle(dealership.getName());

        editButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                presenter.showForm(dealership);
                return false;
            }
        });
        deleteButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                presenter.deleteClicked();
                return false;
            }
        });
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showCantDelete() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.cant_delete));
        alertDialogBuilder
                .setMessage(getString(R.string.shop_associated_with_car))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok_button),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void showDeleteWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.shop_delete_title));
        alertDialogBuilder
                .setMessage(getString(R.string.shop_delete_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                        presenter.removeShop(dealership);
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
}

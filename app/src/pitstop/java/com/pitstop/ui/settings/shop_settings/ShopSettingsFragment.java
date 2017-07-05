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

/**
 * Created by Matthew on 2017-06-26.
 */

public class ShopSettingsFragment extends PreferenceFragment implements ShopSettingsInterface {
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

        presenter = new ShopSettingsPresenter(switcher,component);
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
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT);
    }

    @Override
    public void showCantDelete() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle("Can't Delete");
        alertDialogBuilder
                .setMessage("This shop is associated with one of your cars")
                .setCancelable(false)
                .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
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
        alertDialogBuilder.setTitle("Delete this shop");
        alertDialogBuilder
                .setMessage("Are you sure you want to delete this shop?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                        presenter.removeShop(dealership);
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
}

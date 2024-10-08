package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypeFragment extends Fragment implements ShopTypeView {
    private ShopTypePresenter presenter;
    private CustomShopActivityCallback switcher;

    private Context context;
    private GlobalApplication application;

    private Car car;

    private MixpanelHelper mixpanelHelper;

    @BindView(R.id.pitstop_shop_button)
    CardView selectPitstopShop;

    @BindView(R.id.custom_shop_button)
    CardView selectCustomShop;

    @BindView(R.id.no_shop_button)
    CardView selectNoShop;

    public void setCar(Car car){
        this.car = car;
    }

    @Override
    public void setSwitcher(CustomShopActivityCallback switcher){
        this.switcher = switcher;
    }
    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_shop_type, container, false);
        ButterKnife.bind(this,view);
        selectPitstopShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setViewPitstopShops();
            }
        });
        selectCustomShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setViewShopSearch();
            }
        });
        selectNoShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               presenter.showNoShopWarning();
            }
        });

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new ShopTypePresenter(switcher,component,mixpanelHelper);
        presenter.subscribe(this);

        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void noShopWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());// not sure if this is allowed
        alertDialogBuilder.setTitle(getString(R.string.no_shop_alert_dialog_title));
        alertDialogBuilder
                .setMessage(getString(R.string.no_shop_alert_dialog_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        presenter.setCarNoDealer(car);
                        dialog.cancel();

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
    public void displayError(String message) {
        Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
    }
}

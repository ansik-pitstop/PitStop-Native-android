package com.pitstop.ui.custom_shops.view_fragments.ShopType;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.custom_shops.CustomShopActivity;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.my_trips.MyTripsActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by matt on 2017-06-07.
 */

public class ShopTypeFragment extends Fragment implements ShopTypeInterface {
    private ShopTypePresenter presenter;
    private FragmentSwitcherInterface switcher;

    @BindView(R.id.pitstop_shop_button)
    CardView selectPitstopShop;

    @BindView(R.id.custom_shop_button)
    CardView selectCustomShop;

    @BindView(R.id.no_shop_button)
    CardView selectNoShop;

    @Override
    public void setSwitcher(FragmentSwitcherInterface switcher){
        this.switcher = switcher;
    }
    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
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
        presenter = new ShopTypePresenter();
        presenter.subscribe(this,switcher);
        presenter.setupFragment();
        return view;
    }

    @Override
    public void noShopWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());// not sure if this is allowed
        alertDialogBuilder.setTitle("Are you sure you do not wish to select a shop");
        alertDialogBuilder
                .setMessage("You won't be able to request services. You can add a shop in the settings page at any time")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {

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

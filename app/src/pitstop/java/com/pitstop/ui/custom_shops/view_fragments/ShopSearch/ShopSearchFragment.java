package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;


import com.google.android.gms.maps.model.LatLng;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopAdapter;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchFragment extends Fragment implements ShopSearchView {
    private ShopSearchPresenter presenter;
    private CustomShopActivityCallback switcher;

    private Context context;
    private GlobalApplication application;

    private ShopAdapter shopAdapter;

    private Car car;

    private LatLng location;

    private MixpanelHelper mixpanelHelper;

    @BindView(R.id.shop_search_progress)
    ProgressBar shopSearchProgress;
    @BindView(R.id.myshop_search_progress)
    ProgressBar myShopSearchProgress;

    @BindView(R.id.my_shops_list)
    RecyclerView myShopsList;
    @BindView(R.id.pitstop_search_list)
    RecyclerView pitstopShops;
    @BindView(R.id.search_results_list)
    RecyclerView searchResults;

    @BindView(R.id.my_shops_category)
    CardView shopCategory;
    @BindView(R.id.pitstop_category)
    CardView pitstopCategory;
    @BindView(R.id.search_results_category)
    CardView searchCategory;


    @BindView(R.id.search_bar)
    SearchView searchBar;

    @BindView(R.id.add_own_button)
    CardView addOwnButton;

    @Override
    public void setSwitcher(CustomShopActivityCallback switcher) {
        this.switcher = switcher;
    }

    public void setCar(Car car){
        this.car = car;
    }
    public void setLocation(LatLng location){
        this.location = location;
    }

    @Override
    public LatLng getLocation() {
        return location;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_shop_search, container, false);
        ButterKnife.bind(this,view);
        addOwnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setViewShopForm(null);
            }
        });
        searchBar.setFocusableInTouchMode(true);
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                presenter.filterLists(newText);
                return false;
            }
        });



        pitstopShops.setNestedScrollingEnabled(false);
        myShopsList.setNestedScrollingEnabled(false);
        searchResults.setNestedScrollingEnabled(false);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new ShopSearchPresenter(switcher,component,mixpanelHelper);
        presenter.subscribe(this);



        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.focusSearch();
            }
        });
        presenter.getMyShops();
        presenter.getPitstopShops();
        return view;
    }

    @Override
    public void loadingMyShops(boolean show) {
        if(show){
            myShopSearchProgress.setVisibility(View.VISIBLE);
        }else{
            myShopSearchProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadingGoogle(boolean show) {
        if(show){
            shopSearchProgress.setVisibility(View.VISIBLE);
        }else{
            shopSearchProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void onResume() {
        super.onResume();
        searchBar.setQuery("",true);
    }

    @Override
    public void focusSearch() {
        searchBar.onActionViewExpanded();
    }

    @Override
    public void unFocusSearch() {
        searchBar.onActionViewCollapsed();
    }

    @Override
    public void showPitstopCategory(boolean show) {
        if(show){
            pitstopCategory.setVisibility(View.VISIBLE);
            pitstopShops.setVisibility(View.VISIBLE);
            return;
        }
        pitstopShops.setVisibility(View.GONE);
        pitstopCategory.setVisibility(View.GONE);

    }

    @Override
    public void showSearchCategory(boolean show) {
        if(show){
            searchCategory.setVisibility(View.VISIBLE);
            searchResults.setVisibility(View.VISIBLE);
            return;
        }
        searchCategory.setVisibility(View.GONE);
        searchResults.setVisibility(View.GONE);
    }

    @Override
    public void showShopCategory(boolean show) {
        if(show){
            myShopsList.setVisibility(View.VISIBLE);
            shopCategory.setVisibility(View.VISIBLE);
            return;
        }
        myShopsList.setVisibility(View.GONE);
        shopCategory.setVisibility(View.GONE);

    }

    @Override
    public void setUpMyShopsList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        myShopsList.setAdapter(shopAdapter);
        myShopsList.setLayoutManager(linearLayoutManager);

    }
    @Override
    public void showConfirmation(Dealership dealership) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.set_shop_alert_intro)+dealership.getName());
        alertDialogBuilder
                .setMessage(R.string.change_shop_alert_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes_button_text,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        presenter.changeShop(dealership);
                    }
                })
                .setNegativeButton(R.string.no_button_text,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();

                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void setUpPitstopList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        pitstopShops.setAdapter(shopAdapter);
        pitstopShops.setLayoutManager(linearLayoutManager);

    }

    @Override
    public void setUpSearchList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        searchResults.setAdapter(shopAdapter);
        searchResults.setLayoutManager(linearLayoutManager);
    }
}


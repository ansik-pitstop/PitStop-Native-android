package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private final String TAG = getClass().getSimpleName();

    private ShopSearchPresenter presenter;
    private CustomShopActivityCallback switcher;

    private Context context;
    private GlobalApplication application;

    private ShopAdapter shopAdapter;
    private Car car;
    private LatLng location;

    private MixpanelHelper mixpanelHelper;

    @BindView(R.id.shop_search_progress)
    View shopSearchProgress;

    @BindView(R.id.search_results_list)
    RecyclerView searchResults;

    @BindView(R.id.search_results_category)
    CardView searchCategory;

    @BindView(R.id.search_bar)
    SearchView searchBar;

    @Override
    public void setSwitcher(CustomShopActivityCallback switcher) {
        Log.d(TAG,"setSwitcher()");
        this.switcher = switcher;
    }

    public void setCar(Car car) {
        Log.d(TAG,"setCar() car: "+car);
        this.car = car;
    }
    public void setLocation(LatLng location) {
        Log.d(TAG,"setLocation() location.lat: "+location.latitude
                +", location.long: "+location.longitude);
        this.location = location;
    }

    @Override
    public LatLng getLocation() {
        Log.d(TAG,"getLocation() location.lat: "+location.latitude
                +", location.long: "+location.longitude);
        return location;
    }

    @Override
    public Car getCar() {
        Log.d(TAG,"getCar()");
        return car;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_shop_search, container, false);
        ButterKnife.bind(this,view);
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

        searchResults.setNestedScrollingEnabled(false);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        mixpanelHelper = new MixpanelHelper(application);

        presenter = new ShopSearchPresenter(switcher,component,mixpanelHelper);
        presenter.subscribe(this);
        presenter.filterLists("");



        searchBar.setOnClickListener(v -> presenter.focusSearch());
        return view;
    }

    @Override
    public void loadingGoogle(boolean show) {
        Log.d(TAG,"loadingGoogle() show: "+show);
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
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void focusSearch() {
        Log.d(TAG,"focusSearch()");
        searchBar.onActionViewExpanded();
    }

    @Override
    public void unFocusSearch() {
        Log.d(TAG,"unfocusSearch()");
        searchBar.onActionViewCollapsed();
    }

    @Override
    public void showConfirmation(Dealership dealership) {
        Log.d(TAG,"showConfirmation() dealership: "+dealership.getName());
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle(getString(R.string.set_shop_alert_intro)+dealership.getName());
        alertDialogBuilder
                .setMessage(R.string.change_shop_alert_message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok_button), (dialog, id) -> {
                    dialog.cancel();
                    presenter.changeShop(dealership);
                })
                .setNegativeButton(getString(R.string.no_button_text), (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    @Override
    public void setUpSearchList(List<Dealership> dealerships) {
        Log.d(TAG,"setUpSearchList() dealerships: "+dealerships);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        searchResults.setAdapter(shopAdapter);
        searchResults.setLayoutManager(linearLayoutManager);
    }
}


package com.pitstop.ui.custom_shops;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;
import com.pitstop.R;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.view_fragments.PitstopShops.PitstopShopsFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopForm.ShopFormFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopSearch.ShopSearchFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopType.ShopTypeFragment;

import static com.pitstop.ui.add_car_old.AddCarActivity.ADD_CAR_SUCCESS;

/**
 * Created by matt on 2017-06-07.
 */

public class CustomShopActivity extends AppCompatActivity implements CustomShopView,CustomShopActivityCallback{

    private final String TAG = getClass().getSimpleName();

    public static final String CAR_EXTRA = "car";
    public static final String START_SOURCE_EXTRA = "start_source";

    private ShopTypeFragment shopTypeFragment;
    private ShopSearchFragment shopSearchFragment;
    private PitstopShopsFragment pitstopShopsFragment;
    private ShopFormFragment shopFormFragment;
    private CustomShopPresenter presenter;
    private FragmentManager fragmentManager;
    private String currentViewName = "";

    private  LatLng location;

    private Car car;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_shop);

        car = getIntent().getParcelableExtra(CAR_EXTRA);
        String startSource  = getIntent().getStringExtra(START_SOURCE_EXTRA);
        if (startSource == null) startSource = "";

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria,true);
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(provider, 1, 1,locationListener);
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if(lastKnownLocation != null){location = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());}
        }
        locationManager.removeUpdates(locationListener);
        locationManager = null;


        fragmentManager = getFragmentManager();
        shopSearchFragment = new ShopSearchFragment();
        shopTypeFragment = new ShopTypeFragment();
        pitstopShopsFragment = new PitstopShopsFragment();
        shopFormFragment = new ShopFormFragment();
        shopSearchFragment.setSwitcher(this);
        shopSearchFragment.setCar(car);
        shopSearchFragment.setLocation(location);
        shopTypeFragment.setSwitcher(this);
        shopTypeFragment.setCar(car);
        pitstopShopsFragment.setSwitcher(this);
        shopFormFragment.setCar(car);
        shopFormFragment.setUpdate(false);
        pitstopShopsFragment.setCar(car);
        shopFormFragment.setSwitcher(this);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getApplicationContext()))
                .build();

        presenter = new CustomShopPresenter(this,component,startSource);
        presenter.subscribe(this);
        presenter.setViewCustomShop();
        presenter.setUpNavBar();
    }

    @Override
    public void back() {
        super.onBackPressed();
    }

    @Override
    public String getCurrentViewName() {
        return currentViewName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");

        if (presenter != null && !presenter.onBackPressed()){
            super.onBackPressed();
        }
    }

    @Override
    public void setUpNavBar(boolean displayHomeButton) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(displayHomeButton);
    }

    @Override
    public void setViewShopType() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopTypeFragment);
        fragmentTransaction.commit();
        currentViewName = VIEW_SHOP_TYPE;
    }

    @Override
    public void setViewSearchShop() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopSearchFragment);
        fragmentTransaction.addToBackStack("search_shop");
        fragmentTransaction.commit();
        currentViewName = VIEW_SHOP_SEARCH;
    }

    @Override
    public void setViewPitstopShops() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, pitstopShopsFragment);
        fragmentTransaction.addToBackStack("pitstop_shops");
        fragmentTransaction.commit();
        currentViewName = VIEW_SHOP_PITSTOP;
    }

    @Override
    public void setViewShopForm(Dealership dealership) {
        shopFormFragment.setDealership(dealership);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopFormFragment);
        fragmentTransaction.addToBackStack("shop_form");
        fragmentTransaction.commit();
        currentViewName = VIEW_SHOP_FORM;
    }

    @Override
    public void endCustomShops() {
        Intent intent = new Intent();
        intent.putExtra(CAR_EXTRA,car);
        setResult(ADD_CAR_SUCCESS,intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }
}

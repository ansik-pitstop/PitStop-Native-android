package com.pitstop.ui.custom_shops;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.view_fragments.PitstopShops.PitstopShopsFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopForm.ShopFormFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopSearch.ShopSearchFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopType.ShopTypeFragment;
import com.pitstop.utils.MixpanelHelper;


import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by matt on 2017-06-07.
 */

public class CustomShopActivity extends AppCompatActivity implements CustomShopView,CustomShopActivityCallback{
    private ShopTypeFragment shopTypeFragment;
    private ShopSearchFragment shopSearchFragment;
    private PitstopShopsFragment pitstopShopsFragment;
    private ShopFormFragment shopFormFragment;
    private CustomShopPresenter presenter;
    private FragmentManager fragmentManager;


    private  LatLng location;

    private Car car;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_shop);

        car = getIntent().getParcelableExtra(CAR_EXTRA);


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
        }
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if(lastKnownLocation != null){location = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());}
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

        presenter = new CustomShopPresenter(this);
        presenter.subscribe(this);
        presenter.setViewCustomShop();
        presenter.setUpNavBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void setUpNavBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setViewShopType() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopTypeFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewSearchShop() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopSearchFragment);
        fragmentTransaction.addToBackStack("search_shop");
        fragmentTransaction.commit();
    }

    @Override
    public void setViewPitstopShops() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, pitstopShopsFragment);
        fragmentTransaction.addToBackStack("pitstop_shops");
        fragmentTransaction.commit();
    }

    @Override
    public void setViewShopForm(Dealership dealership) {
        shopFormFragment.setDealership(dealership);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopFormFragment);
        fragmentTransaction.addToBackStack("shop_form");
        fragmentTransaction.commit();
    }

    @Override
    public void endCustomShops() {
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
    protected void onStop() {
        super.onStop();
        presenter.unsubscribe();
    }
}

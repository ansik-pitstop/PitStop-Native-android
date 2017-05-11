package com.pitstop.ui.my_trips;


import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.pitstop.R;
import com.pitstop.ui.my_trips.view_fragments.AddTrip;
import com.pitstop.ui.my_trips.view_fragments.TripHistory;
import com.pitstop.ui.my_trips.view_fragments.TripView;


/**
 * Created by Matthew on 2017-05-09.
 */

public class MyTripsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private TripHistory tripHistory;
    private AddTrip addTrip;
    private TripView tripView;
    private FragmentManager fragmentManager;

    private SupportMapFragment supMapFragment;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;





    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        supMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_trip_map));
        supMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                getMyLocation();
            }
        });

        fragmentManager = getFragmentManager();
        tripHistory = new TripHistory();
        addTrip = new AddTrip();
        tripView = new TripView();
        supMapFragment.getView().setVisibility(View.GONE);
        setViewTripHistory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @SuppressWarnings("all")
    void getMyLocation() {
        if (googleMap != null) {
            // Now that map has loaded, let's get our location!
            googleMap.setMyLocationEnabled(true);
            mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
            connectClient();
        }

    }


    protected void connectClient() {
        //if (isGooglePlayServicesAvailable() && mGoogleApiClient != null) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        connectClient();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        startLocationUpdates();
    }
    @SuppressWarnings("all")
    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public void onLocationChanged(Location location) {

    }



    public void setViewTripHistory(){
        getSupportActionBar().setTitle("Trip History");
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, tripHistory);
        fragmentTransaction.commit();
    }

    public void setViewTripView(){
        getSupportActionBar().setTitle("Trip View");
        supMapFragment.getView().setVisibility(View.VISIBLE);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, tripView);
        fragmentTransaction.commit();
    }

    public void setViewAddTrip(){
        getSupportActionBar().setTitle("Add Trip");
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, addTrip);
        fragmentTransaction.commit();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(tripHistory.isVisible()){
            super.onBackPressed();
        } else if(addTrip.isVisible()){
            setViewTripHistory();
        } else if(tripView.isVisible()){
            supMapFragment.getView().setVisibility(View.GONE);
            setViewAddTrip();
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
    }


}

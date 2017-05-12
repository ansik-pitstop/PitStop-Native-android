package com.pitstop.ui.my_trips;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;


import android.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.R;
import com.pitstop.ui.my_trips.view_fragments.AddTrip;
import com.pitstop.ui.my_trips.view_fragments.TripHistory;
import com.pitstop.ui.my_trips.view_fragments.TripView;




/**
 * Created by Matthew on 2017-05-09.
 */

public class MyTripsActivity extends AppCompatActivity{

    private TripHistory tripHistory;
    private AddTrip addTrip;
    private TripView tripView;
    private FragmentManager fragmentManager;

    private SupportMapFragment supMapFragment;
    private GoogleMap googleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Criteria criteria;
    private String provider;


    Location lastKnownLocation;

    private static final long MIN_TIME = 10;
    private static final float MIN_DISTANCE = 20;






    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationChanged(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria,true);
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(provider,MIN_TIME,MIN_DISTANCE,locationListener);
        }

        supMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_trip_map));
        supMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                getInitialLocation();
            }
        });

        fragmentManager = getFragmentManager();
        tripHistory = new TripHistory();
        addTrip = new AddTrip();
        tripView = new TripView();
        supMapFragment.getView().setVisibility(View.GONE);
        setViewTripHistory();
    }


    private void locationChanged(Location location){
        if((ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) || lastKnownLocation == null){
            return;
        }
        drawLineOnMap(lastKnownLocation,location);
        zoomOnUser(location);
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
    }

    private void drawLineOnMap(Location start, Location end){
        PolylineOptions line = new PolylineOptions();
        LatLng latlngStart = new LatLng(start.getLatitude(), start.getLongitude());
        LatLng latlngEnd =  new LatLng(end.getLatitude(), end.getLongitude());
        line.add(latlngStart);
        line.add(latlngEnd);
        googleMap.addPolyline(line);
    }


    void getInitialLocation() {
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (googleMap != null && lastKnownLocation != null) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            snapCamera(lastKnownLocation);
        }

    }
    public void snapCamera(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.moveCamera(cameraUpdate);

    }


    public void zoomOnUser(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.animateCamera(cameraUpdate);

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

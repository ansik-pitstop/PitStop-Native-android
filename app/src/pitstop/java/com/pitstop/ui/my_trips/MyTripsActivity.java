package com.pitstop.ui.my_trips;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import android.location.LocationListener;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalTripAdapter;
import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.models.Trip;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.my_trips.view_fragments.AddTrip;
import com.pitstop.ui.my_trips.view_fragments.PrevTrip;
import com.pitstop.ui.my_trips.view_fragments.TripHistory;
import com.pitstop.ui.my_trips.view_fragments.TripView;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Matthew on 2017-05-09.
 */

public class MyTripsActivity extends AppCompatActivity{

    private TripHistory tripHistory;
    private PrevTrip prevTripView;
    private AddTrip addTrip;
    private TripView tripView;
    private FragmentManager fragmentManager;
    private GlobalApplication application;

    private Geocoder geocoder;
    private SupportMapFragment supMapFragment;
    private GoogleMap googleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Criteria criteria;
    private String provider;

    private LocalTripAdapter localTripAdapter;
    private boolean tripStarted;
    private Car dashboardCar;
    private MenuItem endTripButton;
    private Location lastKnownLocation;
    private double totalDistance;
    private NetworkHelper networkHelper;
    private Trip trip;

    private static final long MIN_TIME = 10;
    private static final float MIN_DISTANCE = 1;

    private static final double KMH_FACTOR = 3600/1000;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        application = (GlobalApplication) getApplicationContext();
        localTripAdapter = new LocalTripAdapter(application);
        networkHelper = new NetworkHelper(application);
        geocoder = new Geocoder(application);
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        tripStarted = false;


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
        fragmentManager = getFragmentManager();
        tripHistory = new TripHistory();
        prevTripView = new PrevTrip();
        addTrip = new AddTrip();
        tripView = new TripView();

        supMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_trip_map));
        supMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                getInitialLocation();
            }
        });
        setViewTripHistory();
    }


    private void locationChanged(Location location){
        if((ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)
                || lastKnownLocation == null || !tripStarted){
            return;
        }
        drawLineOnMap(lastKnownLocation,location);
        zoomLocation(location);
        trip.addPoint(location);
        accumulateDistance(lastKnownLocation,location);// maybe put this into the trip object
        //System.out.println("testing "+location.getSpeed() + totalDistance);
        tripView.setSpeed(location.getSpeed()*KMH_FACTOR);// this is actually currently in m/s need to change to km/h
        tripView.setDistance(totalDistance);
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
    public Car getDashboardCar(){
        return dashboardCar;
    }


    private void getInitialLocation() {
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
    }

    public String getAddress(Location location){//might want to contextually change this
        List<Address> addresses = null;
        try{
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
        } catch (IOException e){
            e.printStackTrace();
        }
        if(addresses.get(0) != null){
            return addresses.get(0).getAddressLine(0)+" "+addresses.get(0).getAddressLine(1)+" "+addresses.get(0).getAddressLine(2);
        }else{
            return "No Address Available";
        }
    }

    private void accumulateDistance(Location start, Location end){
        totalDistance += start.distanceTo(end);
    }

    private void snapCamera(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.moveCamera(cameraUpdate);
    }


    private void zoomLocation(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.animateCamera(cameraUpdate);

    }

    public void leaveTrip(){
        if(endTripButton != null){ endTripButton.setVisible(false);}
        supMapFragment.getView().setVisibility(View.GONE);
        if(googleMap != null){
            if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
                googleMap.setMyLocationEnabled(false);
            }
            googleMap.clear();
        }
        tripStarted = false;


    }


    public void setViewTripHistory(){
        leaveTrip();
        getSupportActionBar().setTitle("Trip History");
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, tripHistory);
        fragmentTransaction.commit();
    }

    public void setViewPrevTrip(Trip prevTrip){
        leaveTrip();
        getSupportActionBar().setTitle("Previous Trip");
        supMapFragment.getView().setVisibility(View.VISIBLE);
        PolylineOptions prevPath = new PolylineOptions();
        List<Location> prevLocations  = prevTrip.getPath();
        for(Location loc : prevLocations){
            prevPath.add(new LatLng(loc.getLatitude(),loc.getLongitude()));
        }
        googleMap.addPolyline(prevPath);
        snapCamera(prevTrip.getStart());
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, prevTripView);
        fragmentTransaction.commit();
    }

    public void setViewTripView(){//basically trip started
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            googleMap.setMyLocationEnabled(true);
        }
        snapCamera(lastKnownLocation);
        trip = new Trip();
        getSupportActionBar().setTitle("Trip View");
        tripStarted = true;
        endTripButton.setVisible(true);
        supMapFragment.getView().setVisibility(View.VISIBLE);
        tripView.setAddress(getAddress(lastKnownLocation));
        trip.setStartAddress(getAddress(lastKnownLocation));
        trip.setStart(lastKnownLocation);
        trip.addPoint(lastKnownLocation);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.trip_view_holder, tripView);
        fragmentTransaction.commit();
    }

    public void setViewAddTrip(){
        leaveTrip();
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
        }else if(id == R.id.end_trip){//end trip
            trip.setEnd(lastKnownLocation);
            trip.setEndAddress(getAddress(lastKnownLocation));
            trip.setTotalDistance(totalDistance);

            networkHelper.postTripStep1(trip,dashboardCar.getVin(),new RequestCallback() {// yes this is messed up
                @Override
                public void done(String response, RequestError requestError) {
                    JSONObject jObject = null;
                    //System.out.println("Testing Response 1" + response);
                    if(response != null && requestError == null) {
                        try {
                            jObject = new JSONObject(response);
                            trip.setTripId(jObject.getInt("id"));
                            //System.out.println("Testing " + trip.getId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        localTripAdapter.storeTripData(trip);
                        tripHistory.setupList();
                        System.out.println("testing "+localTripAdapter.getAllTrips());
                       /* networkHelper.postTripStep2(trip,new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                System.out.println("Testing Response 2" + response);
                                System.out.println("Testing Response 2 Error" + requestError.getMessage());
                                networkHelper.putTripStep3(trip, new RequestCallback() {
                                    @Override
                                    public void done(String response, RequestError requestError) {
                                        System.out.println("Testing Response 3" + response);

                                    }
                                });

                            }
                        });*/
                    }
                }

            });

            setViewTripHistory();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_end_trip, menu);
        endTripButton = menu.findItem(R.id.end_trip);
        endTripButton.setVisible(false);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(tripHistory.isVisible()){
            super.onBackPressed();
        } else if(addTrip.isVisible()){
            setViewTripHistory();
        } else if(tripView.isVisible()){
            setViewAddTrip();
        }else if(prevTripView.isVisible()){
            setViewTripHistory();
        }

    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
    }
}

package com.pitstop.ui.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.pitstop.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Matthew on 2017-05-29.
 */

public class TripService extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;
    private Criteria criteria;
    private String provider;
    private List<Location> locations;
    private final int MIN_TIME = 1000;
    private final int MIN_DISTANCE = 100;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);


        System.out.println("Testing service started");

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
        locations = new ArrayList<>();

        return START_STICKY;
    }

    private void locationChanged(Location location){
        Log.d("Testing","Location service called");
        locations.add(location);
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent("com.pitstop.TRIP_BROADCAST");
        Gson gson = new Gson();
        String jsonData;
        jsonData = gson.toJson(locations);
        intent.putExtra("Locations",jsonData);
        sendBroadcast(intent);
        System.out.println("Testing service ended");

    }
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


}

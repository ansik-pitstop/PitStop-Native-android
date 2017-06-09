package com.pitstop.ui.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import com.google.gson.Gson;
import com.pitstop.R;
import com.pitstop.models.Trip;
import com.pitstop.models.TripLocation;
import com.pitstop.ui.my_trips.MyTripsActivity;

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
    private final int MIN_TIME = 2000;
    private final int MIN_DISTANCE = 100;
    private Trip trip;
    private String stringTrip;
    private Gson gson = new Gson();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(intent == null){
            return START_REDELIVER_INTENT;
        }
        Intent notificationIntent = new Intent(getApplicationContext(), MyTripsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentTitle("Pitstop Trip Tracking");
        builder.setContentText("Your trip is still being tracked");
        builder.setTicker("Fancy Notification");
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.ic_directions_car_white_24dp);
        builder.setOngoing(true);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.notify(154, notification);

        stringTrip = intent.getStringExtra("Trip");

        trip = gson.fromJson(stringTrip,Trip.class);


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
        registerLocationListener();
        locations = new ArrayList<>();

        return START_STICKY;
    }

    private void locationChanged(Location location){
        trip.addPoint(new TripLocation(location));
    }

    public void unregisterLocationListener(){
        locationManager.removeUpdates(locationListener);
        locationManager = null;
    }
    public void registerLocationListener(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria,true);
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(provider,MIN_TIME,MIN_DISTANCE,locationListener);
        }
    }

    @Override
    public void onDestroy() {
        unregisterLocationListener();
        Intent intent = new Intent("com.pitstop.TRIP_BROADCAST");
        String jsonData;
        jsonData = gson.toJson(trip);
        intent.putExtra("Trip",jsonData);
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.cancel(154);
        sendBroadcast(intent);

    }
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


}

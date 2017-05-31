package com.pitstop.ui.my_trips;



import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;


import android.location.LocationListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalTripAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.Trip;
import com.pitstop.models.TripLocation;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.my_trips.view_fragments.AddTrip;
import com.pitstop.ui.my_trips.view_fragments.PrevTrip;
import com.pitstop.ui.my_trips.view_fragments.TripHistory;
import com.pitstop.ui.my_trips.view_fragments.TripView;
import com.pitstop.ui.services.TripService;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Semaphore;


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

    private boolean tripStarted = false;
    private Car dashboardCar;
    private MenuItem endTripButton;
    private MenuItem shareTripButton;
    private Location lastKnownLocation;
    private double totalDistance;
    private NetworkHelper networkHelper;
    private Trip trip;
    private List<Trip> locallyStoredTrips;
    private BroadcastReceiver broadcastReceiver;
    private LocalTripAdapter localTripAdapter;



    private static final long MIN_TIME = 1000;
    private static final float MIN_DISTANCE = 10;

    private BitmapDescriptor startIcon;
    private BitmapDescriptor endIcon;
    private int lineColor;
    private boolean isMerc;
    public ProgressDialog loading;
    boolean isTaskRunning;


    private static final double KMH_FACTOR = 3600/1000;

    private void setMerc(){
        startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_annotation_mercedes);
        endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_annotation_mercedes);
        lineColor = Color.BLACK;
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this , R.color.black));
        }
        isMerc = true;
    }
    public int getLineColor(){return lineColor;}

    public boolean getMerc(){return isMerc;}




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        application = (GlobalApplication) getApplicationContext();

        localTripAdapter = new LocalTripAdapter(this);
        locallyStoredTrips = localTripAdapter.getAllTrips();

        //localTripAdapter = new LocalTripAdapter(this);
        //localTripAdapter.getAllTrips();


        networkHelper = new NetworkHelper(application);
        geocoder = new Geocoder(application);
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        tripStarted = false;

        if(dashboardCar.getDealership() != null) {
            if (BuildConfig.DEBUG && (dashboardCar.getDealership().getId() == 4 || dashboardCar.getDealership().getId() == 18)) {
                setMerc();
            } else if (!BuildConfig.DEBUG && dashboardCar.getDealership().getId() == 14) {
                setMerc();
            } else {
                TypedValue defaultColor = new TypedValue();
                getTheme().resolveAttribute(R.attr.colorPrimary, defaultColor, true);
                startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_annotation);
                endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_annotation);
                lineColor = defaultColor.data;
                isMerc = false;
            }
        }else{
            TypedValue defaultColor = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorPrimary, defaultColor, true);
            startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_annotation);
            endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_annotation);
            lineColor = defaultColor.data;
            isMerc = false;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(tripStarted){locationChanged(location);}
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
               // System.out.println("Testing status changed "+ bundle.get("satellites"));
            }
            @Override
            public void onProviderEnabled(String s) {

            }
            @Override
            public void onProviderDisabled(String s) {
                if(tripStarted){
                    Toast.makeText(application, "Location services lost trip saved", Toast.LENGTH_SHORT).show();
                    endTrip();
                }
                //System.out.println("Testing provider disabled "+ s);
            }
        };
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria,true);
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(provider,MIN_TIME,MIN_DISTANCE,locationListener);
        }
        broadcastReceiver = new BroadcastReceiver() {//come out of background
            @Override
            public void onReceive(Context context, Intent intent) {
                Gson gson = new Gson();
                Type locListType = new TypeToken<List<TripLocation>>(){}.getType();
                List<Location> locations = gson.fromJson(intent.getStringExtra("Location"),locListType);
                if(locations != null && locations.size()>0) {
                    updateFromBackground(locations);
                    zoomLocation(locations.get(locations.size()-1));
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.pitstop.TRIP_BROADCAST");
        registerReceiver(broadcastReceiver,filter);


        fragmentManager = getFragmentManager();
        tripHistory = new TripHistory();
        prevTripView = new PrevTrip();
        addTrip = new AddTrip();
        tripView = new TripView();

        isTaskRunning = false;

        loading = new ProgressDialog(MyTripsActivity.this);
        loading.setMessage("loading");
        loading.hide();

        supMapFragment = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_trip_map));
        supMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                setViewTripHistory();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("Testing onStop");
        if(tripStarted){//start service
            Intent notificationIntent = new Intent(getApplicationContext(), MyTripsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentTitle("Pitstop Trip Tracking");
            builder.setContentText("Your trip is still being tracked");
            builder.setTicker("Fancy Notification");
            builder.setContentIntent(intent);
            builder.setSmallIcon(R.drawable.ic_directions_car_white_24dp);
            builder.setOngoing(true);
            builder.setAutoCancel(true);
            Notification notification = builder.build();
            NotificationManager notificationManger =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManger.notify(154, notification);
            Intent serviceIntent = new Intent(getApplicationContext(),TripService.class);
            startService(serviceIntent);
        }
    }
    @Override
    protected void onRestart() {//get data from service
        super.onRestart();
        if(tripStarted) {
            Intent intent = new Intent(getApplicationContext(), TripService.class);
            stopService(intent);
            System.out.println("Testing onRestart");
        }
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.cancel(154);
    }


    public void removeTrip(Trip trip){
        localTripAdapter.deleteTrip(trip);
        locallyStoredTrips.remove(trip);
    }


    private void locationChanged(Location location){
        if((ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)
                || lastKnownLocation == null){
            return;
        }
        drawLineOnMap(lastKnownLocation,location);
        zoomLocation(location);
        TripLocation tripLoc = new TripLocation(location);
        trip.addPoint(tripLoc);
        accumulateDistance(lastKnownLocation,location);
        //System.out.println("testing "+location.getSpeed() + totalDistance);
        tripView.setSpeed(location.getSpeed()*KMH_FACTOR);
        tripView.setDistance(totalDistance);
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
    }

    private void drawLineOnMap(Location start, Location end){
        PolylineOptions line = new PolylineOptions();
        LatLng latlngStart = new LatLng(start.getLatitude(), start.getLongitude());
        LatLng latlngEnd =  new LatLng(end.getLatitude(), end.getLongitude());
        line.add(latlngStart);
        line.add(latlngEnd);
        line.color(lineColor);
        googleMap.addPolyline(line);
    }
    public Car getDashboardCar(){
        return dashboardCar;
    }


    private boolean getInitialLocation() {
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (lastKnownLocation == null){
            return false;
        }
        googleMap.setMyLocationEnabled(true);
        snapCamera(lastKnownLocation);
        return true;

    }

    public String getAddress(Location location){//might want to contextually change this
        List<Address> addresses = null;
        try{
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            if(addresses.get(0) != null){
                return addresses.get(0).getAddressLine(0)+" "+addresses.get(0).getAddressLine(1)+" "+addresses.get(0).getAddressLine(2);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return "No Address Available";
    }

    private void accumulateDistance(Location start, Location end){
        totalDistance += start.distanceTo(end);
    }

    public void snapCamera(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.moveCamera(cameraUpdate);
    }


    private void zoomLocation(Location location){
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation,15);
        googleMap.animateCamera(cameraUpdate);
    }

    public void leaveMap(){
        if(endTripButton != null ){
            endTripButton.setVisible(false);
        }
        if(supMapFragment.getView() != null){
            getSupportFragmentManager().beginTransaction().hide(supMapFragment).commit();
        }
        if(googleMap != null){
            googleMap.clear();
        }
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
           googleMap.setMyLocationEnabled(false);
        }
        tripStarted = false;
    }


    public void setViewTripHistory(){
        leaveMap();
        tripHistory.setList(locallyStoredTrips);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.trip_view_holder, tripHistory);
        fragmentTransaction.commit();
        getSupportActionBar().setTitle("Trip History");
    }


    public void drawPath(Trip trip) {
        if(trip == null){return;}
        PolylineOptions prevPath = new PolylineOptions();
        List<TripLocation> prevLocations = trip.getPath();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (TripLocation loc : prevLocations) {
            LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
            prevPath.add(latlng);
            builder.include(latlng);
        }
        prevPath.color(lineColor);
        googleMap.addPolyline(prevPath);
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(trip.getStart().getLatitude(), trip.getStart().getLongitude()))
                .icon(startIcon));
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(trip.getEnd().getLatitude(), trip.getEnd().getLongitude()))
                .icon(endIcon));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

     private void updateFromBackground(List<Location> locations){
         PolylineOptions prevPath = new PolylineOptions();
         prevPath.color(lineColor);
         LatLngBounds.Builder builder = new LatLngBounds.Builder();
         for (Location loc : locations) {
             trip.addPoint(new TripLocation(loc));
             LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
             prevPath.add(latlng);
             builder.include(latlng);
         }
         googleMap.addPolyline(prevPath);
     }


    public void setViewPrevTrip(Trip prevTrip){
        prevTripView.setTrip(prevTrip);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.trip_view_holder, prevTripView);
        fragmentTransaction.commit();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.activity_bottom_down_in,R.anim.activity_bottom_down_out).show(supMapFragment).commit();
        getSupportActionBar().setTitle("Previous Trip");
        shareTripButton.setVisible(true);
        drawPath(prevTrip);

    }

    public void setViewTripView(){//basically trip started
        if((ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)){
            finish(); //take them to the dashboard where the app is already asking for location permissions
            return;
        }
        if(!NetworkHelper.isConnected(application)){
            Toast.makeText(application, "Please check your network connection", Toast.LENGTH_SHORT).show();
            return;
        }
        LocationManager lm = (LocationManager)application.getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER ) || !getInitialLocation()){
            Toast.makeText(application, "Unable to get your location", Toast.LENGTH_SHORT).show();
            return;
        }
        loading.show();
        startTrip();
    }

    public void setViewAddTrip(){
        leaveMap();
        if(locallyStoredTrips.size()>0){
            addTrip.setPrevTrip(locallyStoredTrips.get(locallyStoredTrips.size() - 1));
            addTrip.setTripList(locallyStoredTrips);
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.trip_view_holder, addTrip);
        fragmentTransaction.commit();
        getSupportActionBar().setTitle("Add Trip");
    }

    public void startTrip(){
        trip = new Trip();
        tripStarted = true;
        TripLocation tripLoc = new TripLocation(lastKnownLocation);
        snapCamera(lastKnownLocation);
        tripView.setAddress(getAddress(lastKnownLocation));
        trip.setStartAddress(getAddress(lastKnownLocation));
        trip.setStart(lastKnownLocation);
        trip.addPoint(tripLoc);
        networkHelper.postTripStep1(trip,dashboardCar.getVin(),new RequestCallback() {
            @Override
            public void done(String response, final RequestError requestError) {
                JSONObject jObject = null;
                if(response != null && requestError == null) {
                    try {
                        jObject = new JSONObject(response);
                        trip.setTripId(jObject.getInt("id"));
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
                        fragmentTransaction.replace(R.id.trip_view_holder, tripView);
                        fragmentTransaction.commit();
                        getSupportActionBar().setTitle("Trip View");
                        endTripButton.setVisible(true);
                        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.activity_bottom_down_in,R.anim.activity_bottom_down_out).show(supMapFragment).commit();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else if(response == null && requestError != null){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyTripsActivity.this);
                    alertDialogBuilder.setTitle("An Error Has Occurred");
                    alertDialogBuilder
                            .setMessage(requestError.getMessage())
                            .setCancelable(false)
                            .setPositiveButton("Email Us",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                            "mailto","info@pitstopconnect.com", null));
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Trip Start Error");
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Error code: "+requestError.getStatusCode()+"\n"+"Error: "+requestError.getMessage());
                                    startActivity(Intent.createChooser(emailIntent, "Send email..."));
                                }
                            })
                            .setNegativeButton("Dismiss",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    setViewAddTrip();
                }
                loading.hide();
            }
        });

    }

    public void endTrip(){
        trip.setEnd(lastKnownLocation);
        trip.setEndAddress(getAddress(lastKnownLocation));
        trip.setTotalDistance(totalDistance);
        locallyStoredTrips.add(trip);
        localTripAdapter.storeTripData(trip);
        setViewTripHistory();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {

            if (tripHistory.isVisible()) {
                finish();
            } else if (addTrip.isVisible()) {
                setViewTripHistory();
            } else if (tripView.isVisible()) {
                setViewAddTrip();
            } else if (prevTripView.isVisible()) {
                shareTripButton.setVisible(false);
                setViewTripHistory();
            }
        } else if (id == R.id.end_trip) {//end trip
            endTrip();
        } else if (id == R.id.share_trip) {
            GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    if(prevTripView.isVisible()){
                        shareTrip(snapshot);
                    }
                }
            };
            googleMap.snapshot(callback);
        }
        return super.onOptionsItemSelected(item);
    }

    public Bitmap combineImages(Bitmap c, Bitmap s) {
        Bitmap cs = null;
        int width, height = 0;
        if(c.getHeight() > s.getHeight()) {
            width = c.getWidth();
            height = c.getHeight() + s.getHeight();
        } else {
            width = c.getWidth() ;
            height = s.getHeight() +s.getHeight();
        }
        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        comboImage.drawBitmap(s, 0f, 0f, null);
        comboImage.drawBitmap(c, 0f,c.getHeight(), null);
        return cs;
    }

    public void hideShareTrip(){
        shareTripButton.setVisible(false);
    }

    private void shareTrip(Bitmap bitmapMap){
        View v1 = prevTripView.getView();
        v1.setDrawingCacheEnabled(true);
        Bitmap bitmapAll = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);

        Bitmap bitmap = combineImages(bitmapAll,bitmapMap);

        File imagePath = new File(Environment.getExternalStorageDirectory() + "/trip.jpg");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri uri = Uri.fromFile(imagePath);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        String shareBody = prevTripView.getAddresses();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Pitstop Trip");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(sharingIntent, "Share Trip Via"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trip_navbar, menu);
        endTripButton = menu.findItem(R.id.end_trip);
        shareTripButton = menu.findItem(R.id.share_trip);
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
            shareTripButton.setVisible(false);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
    }
}

package com.pitstop.ui.my_trips;


import android.Manifest;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Trip;
import com.pitstop.models.TripLocation;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.main_activity.MainActivity;
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
import java.util.List;
import java.util.Random;


/**
 * Created by Matthew on 2017-05-09.
 */


public class MyTripsActivity extends AppCompatActivity{
    public static final String TAG = MyTripsActivity.class.getSimpleName();

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
    private MenuItem shareTripButton;
    private Location lastKnownLocation;
    private NetworkHelper networkHelper;
    private Trip trip;
    private List<Trip> locallyStoredTrips;
    private BroadcastReceiver broadcastReceiver;
    private LocalTripStorage localTripStorage;

    private Gson gson = new Gson();


    private static final long MIN_TIME = 2000;
    private static final float MIN_DISTANCE = 10;

    private BitmapDescriptor startIcon;
    private BitmapDescriptor endIcon;
    private int lineColor;
    private boolean isMerc;
    public ProgressDialog loading;
    private boolean isTaskRunning;
    private boolean activityActive;


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
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_my_trips);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        application = (GlobalApplication) getApplicationContext();

        localTripStorage = new LocalTripStorage(this);
        locallyStoredTrips = localTripStorage.getAllTrips();

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        networkHelper = tempNetworkComponent.networkHelper();
        geocoder = new Geocoder(application);
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        tripStarted = false;

//        if(dashboardCar != null) {
//            if(dashboardCar.getShopId() > 0){
//                if (BuildConfig.DEBUG && (dashboardCar.getShopId() == 4 || dashboardCar.getShopId() == 18)) {
//                    setMerc();
//                } else if (!BuildConfig.DEBUG && dashboardCar.getShopId() == 14) {
//                    setMerc();
//                } else {
//                    TypedValue defaultColor = new TypedValue();
//                    getTheme().resolveAttribute(R.attr.colorPrimary, defaultColor, true);
//                    startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_annotation);
//                    endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_annotation);
//                    lineColor = defaultColor.data;
//                    isMerc = false;
//                }
//            }
//        }else{
//            TypedValue defaultColor = new TypedValue();
//            getTheme().resolveAttribute(R.attr.colorPrimary, defaultColor, true);
//            startIcon = BitmapDescriptorFactory.fromResource(R.drawable.start_annotation);
//            endIcon = BitmapDescriptorFactory.fromResource(R.drawable.end_annotation);
//            lineColor = defaultColor.data;
//            isMerc = false;
//        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(tripStarted){locationChanged(location);}
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s) {
                if(tripStarted){
                    Toast.makeText(application, getString(R.string.location_lost_trip_saved), Toast.LENGTH_SHORT).show();
                    endTrip();
                }
            }
        };
        //registerLocationListener();
        broadcastReceiver = new BroadcastReceiver() {//come out of background
            @Override
            public void onReceive(Context context, Intent intent) {
               Trip serviceTrip = gson.fromJson(intent.getStringExtra("Trip"),Trip.class);
                if(serviceTrip != null && serviceTrip.getPath().size()>0) {
                    updateFromBackground(serviceTrip);
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
        loading.setMessage(getString(R.string.show_loading_string));
        loading.hide();
        boolean isServiceRunning = isMyServiceRunning(TripService.class);

        supMapFragment = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_trip_map));
        supMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                if(isServiceRunning) {
                    Intent intent = new Intent(getApplicationContext(), TripService.class);
                    stopService(intent);
                }else{
                    setViewTripHistory();
                }
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStart(){
        super.onStart();
        activityActive = true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        activityActive = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterLocationListener();
        if(tripStarted){
            backGroundTrip();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLocationListener();
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
    protected void onRestart() {//get data from service
        super.onRestart();
        Intent intent = new Intent(getApplicationContext(), TripService.class);
        stopService(intent);
    }


    public void removeTrip(Trip trip){
        localTripStorage.deleteTrip(trip);
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
        tripView.setSpeed(location.getSpeed()*KMH_FACTOR);
        tripView.setDistance(trip.getTotalDistance());
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
            if(addresses.size()>0){
                return addresses.get(0).getAddressLine(0)+" "+addresses.get(0).getAddressLine(1)+" "+addresses.get(0).getAddressLine(2);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return "No Address Available";
    }

    private void accumulateDistance(Location start, Location end){
        trip.addDist(start.distanceTo(end));
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
        if(supMapFragment.getView() != null){
            getSupportFragmentManager().beginTransaction().hide(supMapFragment).commit();
        }
        if(googleMap != null){
            googleMap.clear();
        }
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
           googleMap.setMyLocationEnabled(false);
        }
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.home_as_upindicator);
        tripStarted = false;
    }


    public void setViewTripHistory(){
        leaveMap();
        tripHistory.setList(locallyStoredTrips);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.trip_view_holder, tripHistory);
        fragmentTransaction.commit();
        getSupportActionBar().setTitle(getString(R.string.trip_history));
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



     private void updateFromBackground(Trip serviceTrip){//everything you need to do when you resume a trip
         if(!activityActive){
             return;
         }
         //registerLocationListener();
         getInitialLocation();
         trip = serviceTrip;
         tripStarted = true;
         tripView.setAddress(trip.getStartAddress());
         tripView.setStartTime(trip.getStart().getTime());
         getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_24dp);
         FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
         fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
         fragmentTransaction.replace(R.id.trip_view_holder, tripView);
         fragmentTransaction.commit();
         getSupportActionBar().setTitle("Trip View");
         PolylineOptions prevPath = new PolylineOptions();
         prevPath.color(lineColor);
         for (TripLocation loc : trip.getPath()) {
             LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
             prevPath.add(latlng);
         }
         getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.activity_bottom_down_in,R.anim.activity_bottom_down_out).show(supMapFragment).commit();
         googleMap.clear();
         googleMap.addPolyline(prevPath);
     }

    private class GetandDraw extends AsyncTask<Void, Void, Void>{
        private Trip trip;
        private Trip tripWithPath;

        public void setTrip(Trip trip) {
            this.trip = trip;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            tripWithPath = localTripStorage.getTrip(Integer.toString(trip.getId()));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            drawPath(tripWithPath);
            loading.hide();
        }

    }


    public void setViewPrevTrip(Trip prevTrip){
        prevTripView.setTrip(prevTrip);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.trip_view_holder, prevTripView);
        fragmentTransaction.commit();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.activity_bottom_down_in,R.anim.activity_bottom_down_out).show(supMapFragment).commit();
        getSupportActionBar().setTitle(getString(R.string.add_trip_previous));
        shareTripButton.setVisible(true);
        GetandDraw getandDraw = new GetandDraw();
        getandDraw.setTrip(prevTrip);
        getandDraw.execute();
    }

    public void setViewTripView(){//basically trip started
        if((ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)){
            finish(); //take them to the dashboard where the app is already asking for location permissions
            return;
        }
        if(!NetworkHelper.isConnected(application)){
            Toast.makeText(application, getString(R.string.internet_check_error), Toast.LENGTH_SHORT).show();
            return;
        }
        LocationManager lm = (LocationManager)application.getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER ) || !getInitialLocation()){
            Toast.makeText(application, getString(R.string.unable_to_get_location), Toast.LENGTH_SHORT).show();
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
        getSupportActionBar().setTitle(getString(R.string.add_trip));
    }

    public void startTrip(){
        trip = new Trip();
        tripStarted = true;
        TripLocation tripLoc = new TripLocation(lastKnownLocation);
        snapCamera(lastKnownLocation);
        tripView.setAddress(getAddress(lastKnownLocation));
        trip.setStartAddress(getAddress(lastKnownLocation));
        trip.setStart(new TripLocation(lastKnownLocation));
        trip.getStart().setTime(System.currentTimeMillis());
        tripView.setStartTime(trip.getStart().getTime());
        trip.addPoint(tripLoc);
        if(dashboardCar == null){
            Toast.makeText(application, getString(R.string.vin_get_fail), Toast.LENGTH_SHORT).show();
            finish();
        }
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
                        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_24dp);
                        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.activity_bottom_down_in,R.anim.activity_bottom_down_out).show(supMapFragment).commit();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else if(response == null && requestError != null){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyTripsActivity.this);
                    alertDialogBuilder.setTitle(getString(R.string.timeline_error_message));
                    alertDialogBuilder
                            .setMessage(requestError.getMessage())
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.email_us),new DialogInterface.OnClickListener() {
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
        trip.setEnd(new TripLocation(lastKnownLocation));
        trip.setEndAddress(getAddress(lastKnownLocation));
        locallyStoredTrips.add(trip);
        localTripStorage.storeTripData(trip);
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

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyTripsActivity.this);
                alertDialogBuilder.setTitle(getString(R.string.cancel_trip_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.cancel_trip_message))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                               setViewTripHistory();
                            }
                        })
                        .setNegativeButton(getString(R.string.no_button_text),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else if (prevTripView.isVisible()) {
                shareTripButton.setVisible(false);
                setViewTripHistory();
            }
        } else if (id == R.id.share_trip) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        179);
            }else{
                GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap snapshot) {
                        if(prevTripView.isVisible()){
                            Random r = new Random();
                            shareTrip(snapshot,Integer.toString(r.nextInt(1000000)));
                        }
                    }
                };
                googleMap.snapshot(callback);
            }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 179: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap snapshot) {
                            if(prevTripView.isVisible()){
                                Random r = new Random();
                                shareTrip(snapshot,Integer.toString(r.nextInt(1000000)));
                            }
                        }
                    };
                    googleMap.snapshot(callback);
                } else {
                    Toast.makeText(application, getString(R.string.unable_to_share_trip), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    void shareTrip(Bitmap bitmapMap, String fileName){
        View v1 = prevTripView.getView();
        v1.setDrawingCacheEnabled(true);
        Bitmap bitmapAll = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);
        Bitmap bitmap = combineImages(bitmapAll,bitmapMap);

        final String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pitstop";
        File dir = new File(dirPath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        File file = new File(dirPath, fileName+".jpg");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Uri uri = Uri.fromFile(file);
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
        shareTripButton = menu.findItem(R.id.share_trip);
        return true;
    }
    private void backGroundTrip(){
        if(tripStarted){
            Intent serviceIntent = new Intent(getApplicationContext(),TripService.class);
            serviceIntent.putExtra("Trip",gson.toJson(trip));
            startService(serviceIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if(tripHistory.isVisible()){
            finish();
        } else if(addTrip.isVisible()){
            setViewTripHistory();
        } else if(tripView.isVisible()){
            finish();
        }else if(prevTripView.isVisible()){
            setViewTripHistory();
            shareTripButton.setVisible(false);
        }
    }
    
}

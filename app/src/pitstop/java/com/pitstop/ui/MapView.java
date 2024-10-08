package com.pitstop.ui;

/**
 * Created by David C. on 1/3/18.
 */

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.LocationPolyline;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class MapView extends FrameLayout {

    public static final float POLY_WIDTH = 15;
    private final String TAG = MapView.class.getSimpleName();

    private Subject<GoogleMap> mapSubject;

    private MapFragment mMapFragment;

    private MarkerOptions marker;

    public MapView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public MapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MapView(@NonNull Context context, @Nullable AttributeSet attrs,
                   @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        SupportMapFragment mMapFragment = SupportMapFragment.newInstance();

        if (!isInEditMode()) {
            FragmentTransaction fragmentTransaction =
                    ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(getId(), mMapFragment);
            fragmentTransaction.commit();

            mapSubject = BehaviorSubject.create();
            Observable.create(
                    (ObservableOnSubscribe<GoogleMap>) e -> mMapFragment.getMapAsync(e::onNext))
                    .subscribe(mapSubject);
        }
    }

    public void addMarker(double lat, double lon, String title) {
        mapSubject.subscribe(googleMap -> {
            LatLng position = new LatLng(lat, lon);
            marker = new MarkerOptions()
                    .position(position)
                    .title(title);
            googleMap.addMarker(marker);
            //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
        }, err -> {
            Log.e(TAG,"Error adding marker!");
            err.printStackTrace();
        });
    }

    public void addMarker(double lat, double lon, String title, int zoom) {
        mapSubject.subscribe(googleMap -> {
            LatLng position = new LatLng(lat, lon);
            marker = new MarkerOptions()
                    .position(position)
                    .title(title);
            googleMap.addMarker(marker);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
        }, err -> {
            Log.e(TAG,"error adding marker");
            err.printStackTrace();
        });
    }

    public void addPolyline(PolylineOptions polylineOptions) {

        mapSubject.subscribe(googleMap -> {

            googleMap.clear();

            polylineOptions.width(POLY_WIDTH)
                    .geodesic(true)
                    .color(Color.BLUE)
                    .startCap(new RoundCap())
                    .endCap(new RoundCap());

            googleMap.addPolyline(polylineOptions);

            try {

                if (polylineOptions.getPoints().size() > 0) {
                    //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polylineOptions.getPoints().get(0), 16));

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polylineOptions.getPoints()) {
                        builder.include(latLng);
                    }

                    final LatLngBounds bounds = builder.build();

                    //BOUND_PADDING is an int to specify padding of bound.. try 100.
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                    googleMap.animateCamera(cu);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, err -> {
            err.printStackTrace();
            Log.e(TAG,"error adding polyline!");
        });

    }

    public void clearPolyline() {

        mapSubject.subscribe(googleMap -> {

            googleMap.clear();

        }, err -> {
            err.printStackTrace();
            Log.e(TAG,"error clearing polyline");
        });

    }

    public void addPolyline_old(List<LocationPolyline> locationPolyline) {

        mapSubject.subscribe(googleMap -> {

            googleMap.clear();

            PolylineOptions polylineOptions = new PolylineOptions()
                    .width(POLY_WIDTH)
                    .geodesic(true)
                    .color(Color.BLUE);

            for (LocationPolyline location : locationPolyline) {

                if (location.getLocation().size() > 2) { // First Array containing 4 objects inside

                } else { // Arrays that will only contain 2 objects

                    double lat = 0f;
                    double lng = 0f;

                    Location obj1 = location.getLocation().get(0);
                    if (obj1.getTypeId().equalsIgnoreCase("latitude")) {
                        lat = Double.parseDouble(obj1.getData());
                    } else if (obj1.getTypeId().equalsIgnoreCase("longitude")) {
                        lng = Double.parseDouble(obj1.getData());
                    }

                    Location obj2 = location.getLocation().get(1);
                    if (obj2.getTypeId().equalsIgnoreCase("latitude")) {
                        lat = Double.parseDouble(obj2.getData());
                    } else if (obj2.getTypeId().equalsIgnoreCase("longitude")) {
                        lng = Double.parseDouble(obj2.getData());
                    }

                    LatLng latLng = new LatLng(lat, lng);

                    polylineOptions.add(latLng);

                }

            }

            googleMap.addPolyline(polylineOptions);

            try {

                if (polylineOptions.getPoints().size() > 0) {
                    //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polylineOptions.getPoints().get(0), 16));

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polylineOptions.getPoints()) {
                        builder.include(latLng);
                    }

                    final LatLngBounds bounds = builder.build();

                    //BOUND_PADDING is an int to specify padding of bound.. try 100.
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                    googleMap.animateCamera(cu);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"error adding polyline old!");
            }

        }, err -> {
            err.printStackTrace();
            Log.e(TAG,"Error adding polyline old!");
        });

    }

    public void onDestroy() {

        mapSubject = null;

        marker = null;

        mMapFragment = null;

    }

}


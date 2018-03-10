package com.pitstop.ui;

/**
 * Created by David C. on 1/3/18.
 */

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pitstop.models.trip.Location;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class MapView extends FrameLayout {

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

    public void addMarker(double lat, double lon, String title, int zoom) {
        mapSubject.subscribe(googleMap -> {
            LatLng position = new LatLng(lat, lon);
            marker = new MarkerOptions()
                    .position(position)
                    .title(title);
            googleMap.addMarker(marker);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
        });
    }

    public void addPolyline(List<Location> locationList) {



    }

    public void onDestroy() {

        mapSubject = null;

        marker = null;

        mMapFragment = null;

    }

}


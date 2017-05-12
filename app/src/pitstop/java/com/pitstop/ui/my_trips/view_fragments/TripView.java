package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.icu.util.Calendar;
import android.location.Address;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.pitstop.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-10.
 */

public class TripView extends Fragment {

    private String initialAddress;


    public void setAddress(String address){
        initialAddress = address;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        System.out.println("Testing onActivityCreated gets called now");
        Date startTime  = new Date(System.currentTimeMillis());
        TextView startTimeTextView = (TextView) getView().findViewById(R.id.starting_time);
        startTimeTextView.setText(dateFormat(startTime.toString()));
        TextView addressTextView = (TextView) getView().findViewById(R.id.starting_point_address);
        addressTextView.setText(initialAddress);
        super.onActivityCreated(savedInstanceState);
    }

    private String dateFormat(String date){
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.CANADA);
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        try {
            Date formDate = sdf.parse(date);
            String newDate = newFormat.format(formDate);
            return newDate;
        }catch (ParseException e){
            e.printStackTrace();
        }
        return date;
    }
}

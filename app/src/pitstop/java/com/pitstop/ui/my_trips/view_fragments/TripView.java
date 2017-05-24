package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.ui.my_trips.MyTripsActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-10.
 */

public class TripView extends Fragment {

    private String initialAddress;
    private DecimalFormat decimalFormat;
    private TextView currentDistanceTextView;
    private TextView currentSpeedTextView;

    private TextView startTitle;
    private TextView currentTitle;

    private int color;




    public void setAddress(String address){
        initialAddress = address;
    }
    public void setSpeed(double speed){
        if(getView() != null){
            currentSpeedTextView = (TextView) getView().findViewById(R.id.current_speed);
            currentSpeedTextView.setText("Speed: "+decimalFormat.format(speed) + " km/h");
        }
    }
    public void setDistance(double distance){
        if(getView() != null){
            distance /= 1000;//distance is in meters
            currentDistanceTextView = (TextView) getView().findViewById(R.id.current_distance);
            currentDistanceTextView.setText("Distance: " + decimalFormat.format(distance) + " km");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        decimalFormat = new DecimalFormat("0.00");
        Date startTime  = new Date(System.currentTimeMillis());

        color = ((MyTripsActivity)getActivity()).getLineColor();
        startTitle = (TextView) getView().findViewById(R.id.starting_point_title);
        currentTitle = (TextView) getView().findViewById(R.id.current_stats_title);
        startTitle.setTextColor(color);
        currentTitle.setTextColor(color);

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

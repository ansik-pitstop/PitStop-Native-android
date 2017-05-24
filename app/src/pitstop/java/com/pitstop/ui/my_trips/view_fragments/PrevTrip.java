package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Trip;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-17.
 */

public class PrevTrip extends Fragment {

    private Trip tripToShow;

    private TextView startDate;
    private TextView startAddress;
    private TextView endDate;
    private TextView endAddress;
    private TextView totalDistance;
    private DecimalFormat decimalFormat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prev_trip, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        decimalFormat = new DecimalFormat("0.00");

        startDate = (TextView) getView().findViewById(R.id.start_date);
        startAddress = (TextView) getView().findViewById(R.id.start_address);
        endDate = (TextView) getView().findViewById(R.id.end_date);
        endAddress = (TextView) getView().findViewById(R.id.end_address);
        totalDistance = (TextView) getView().findViewById(R.id.trip_distance);

        startDate.setText(dateFormat(tripToShow.getStart().getTime()));
        startAddress.setText(tripToShow.getStartAddress());
        endDate.setText(dateFormat(tripToShow.getEnd().getTime()));
        endAddress.setText(tripToShow.getEndAddress());
        totalDistance.setText(decimalFormat.format(tripToShow.getTotalDistance()/1000)+" km");//total distance is in meters
        super.onActivityCreated(savedInstanceState);


    }

    public void setTrip(Trip trip){
        tripToShow = trip;
    }

    public String getAddresses(){
        if(tripToShow != null){
            return "Starting Address: " + tripToShow.getStartAddress() + "\n \n" +"End Address: "+
                    tripToShow.getEndAddress() +"\n \n" + "Total Distance: "+
                    decimalFormat.format(tripToShow.getTotalDistance()/1000)+" km";
        }
        return "";


    }

    private String dateFormat(Long date){
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        return newFormat.format(date);
    }


}

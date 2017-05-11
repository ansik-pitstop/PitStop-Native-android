package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.my_trips.MyTripsActivity;

/**
 * Created by Matthew on 2017-05-10.
 */

public class TripHistory extends Fragment {
    FloatingActionButton addTripFab;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_trip_history, container, false);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addTripFab = (FloatingActionButton)getView().findViewById(R.id.add_trip_fab);
        addTripFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MyTripsActivity)getActivity()).setViewAddTrip();
            }
        });

    }


}

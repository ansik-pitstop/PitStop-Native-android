package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.my_trips.MyTripsActivity;

/**
 * Created by Matthew on 2017-05-10.
 */

public class AddTrip extends Fragment {
    private ImageButton startTrip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_add_trip, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Car dashCar = ((MyTripsActivity)getActivity()).getDashboardCar();
        TextView carTextView = (TextView)getView().findViewById(R.id.car_for_trip);
        carTextView.setText(dashCar.getYear()+" "+dashCar.getModel());

        startTrip = (ImageButton)getView().findViewById(R.id.start_trip_button);
        startTrip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MyTripsActivity)getActivity()).setViewTripView();
            }
        });
    }

}

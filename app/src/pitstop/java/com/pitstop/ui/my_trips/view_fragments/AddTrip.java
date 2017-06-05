package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.Trip;
import com.pitstop.ui.my_trips.MyTripsActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Matthew on 2017-05-10.
 */

public class AddTrip extends Fragment {
    private ImageButton startTrip;
    private Trip prevTrip;
    private List<Trip> prevTripList;

    private TextView tripCount;
    private TextView totalDistnace;

    private TextView startTime;
    private TextView startAddress;
    private TextView endAddress;
    private TextView tripDistance;
    private DecimalFormat decimalFormat;
    private TextView prevTripTitle;

    private CardView backCard;

    private boolean isMerc;
    private int color;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_add_trip, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Car dashCar = ((MyTripsActivity)getActivity()).getDashboardCar();
        isMerc = ((MyTripsActivity)getActivity()).getMerc();
        color = ((MyTripsActivity)getActivity()).getLineColor();
        TextView carTextView = (TextView)getView().findViewById(R.id.car_for_trip);

        if(dashCar != null){
            carTextView.setText(dashCar.getYear()+" "+dashCar.getModel());
        }
        decimalFormat = new DecimalFormat("0.00");

        backCard = (CardView) getView().findViewById(R.id.back_ground_card);

        tripCount = (TextView) getView().findViewById(R.id.number_of_trips);
        totalDistnace = (TextView) getView().findViewById(R.id.total_distance);
        prevTripTitle = (TextView) getView().findViewById(R.id.previous_trip_title);

        startTime = (TextView) getView().findViewById(R.id.prev_trip_start_time);
        startAddress = (TextView) getView().findViewById(R.id.prev_trip_start_address);
        endAddress = (TextView) getView().findViewById(R.id.prev_trip_end_address);
        tripDistance = (TextView) getView().findViewById(R.id.prev_trip_distance);
        if(prevTripList != null) {
            double totalDist = 0;
            for (Trip curTrip : prevTripList) {
                totalDist += curTrip.getTotalDistance();
            }
            tripCount.setText("Trips made: " + prevTripList.size());
            totalDistnace.setText("Total distance traveled: " + decimalFormat.format(totalDist / 1000) + " km");//to meters
            startTime.setText(dateFormat(prevTrip.getStart().getTime()));
            startAddress.setText(prevTrip.getStartAddress());
            endAddress.setText(prevTrip.getEndAddress());
            tripDistance.setText(decimalFormat.format(prevTrip.getTotalDistance() / 1000) + " km");// total distance in meters
        }else{

        }


        startTrip = (ImageButton)getView().findViewById(R.id.start_trip_button);
        if(isMerc){
            startTrip.setImageResource(R.drawable.start_trip_button_mercedes3x);
        }
        prevTripTitle.setTextColor(color);
        backCard.setCardBackgroundColor(color);
        startTrip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MyTripsActivity)getActivity()).setViewTripView();
                startTrip.setClickable(false);
            }
        });
    }

    public void setPrevTrip(Trip trip){
        prevTrip = trip;
    }

    public void setTripList(List<Trip> trips){
        prevTripList = trips;
    }

    private String dateFormat(Long date){
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        return newFormat.format(date);
    }


}

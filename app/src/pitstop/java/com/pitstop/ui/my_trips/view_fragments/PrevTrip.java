package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Trip;

import com.pitstop.ui.my_trips.MyTripsActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

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
    private Button deleteButton;
    private DecimalFormat decimalFormat;
    private View rootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_prev_trip, container, false);
        setUpUi();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }



    private void setUpUi(){
        decimalFormat = new DecimalFormat("0.00");
        deleteButton = (Button) rootView.findViewById((R.id.delete_trip));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle(getString(R.string.delete_trip_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.delete_trip_message))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes_button_text),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                ((MyTripsActivity)getActivity()).removeTrip(tripToShow);
                                ((MyTripsActivity)getActivity()).setViewTripHistory();
                                ((MyTripsActivity)getActivity()).hideShareTrip();
                            }
                        })
                        .setNegativeButton(getString(R.string.no_button_text),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

            }
        });
        startDate = (TextView) rootView.findViewById(R.id.start_date);
        startAddress = (TextView) rootView.findViewById(R.id.start_address);
        endDate = (TextView) rootView.findViewById(R.id.end_date);
        endAddress = (TextView) rootView.findViewById(R.id.end_address);
        totalDistance = (TextView) rootView.findViewById(R.id.trip_distance);

        startDate.setText(dateFormat(tripToShow.getStart().getTime()));
        startAddress.setText(tripToShow.getStartAddress());
        endDate.setText(dateFormat(tripToShow.getEnd().getTime()));
        endAddress.setText(tripToShow.getEndAddress());
        totalDistance.setText(decimalFormat.format(tripToShow.getTotalDistance()/1000)+ getString(R.string.kilometers_unit));//total distance is in meters
    }




    public void setTrip(Trip trip){
        tripToShow = trip;
    }


    public String getAddresses(){
        if(tripToShow != null){
            return getString(R.string.start_address) + tripToShow.getStartAddress() + "\n \n" +getString(R.string.end_address) +
                    tripToShow.getEndAddress() +"\n \n" + getString(R.string.total_distance_travelled)+
                    decimalFormat.format(tripToShow.getTotalDistance()/1000)+ getString(R.string.kilometers_unit);
        }
        return "";
    }
    private String dateFormat(Long date){
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        return newFormat.format(date);
    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
    }
}

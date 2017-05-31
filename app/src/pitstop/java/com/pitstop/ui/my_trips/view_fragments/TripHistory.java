package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.database.LocalTripAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.Trip;
import com.pitstop.ui.my_trips.MyTripsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Matthew on 2017-05-10.
 */

public class TripHistory extends Fragment {
    private FloatingActionButton addTripFab;

    private RecyclerView mTripsList;
    private TripAdapter mTripAdapter;
    private List<Trip> mTrips;

    private View rootView;
    private int color;


    LinearLayoutManager linearLayoutManager;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_trip_history, container, false);
        color  = ((MyTripsActivity)getActivity()).getLineColor();
        addTripFab = (FloatingActionButton)rootView.findViewById(R.id.add_trip_fab);
        addTripFab.setBackgroundTintList(ColorStateList.valueOf(color));
        addTripFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MyTripsActivity)getActivity()).setViewAddTrip();
            }
        });
        return rootView;
    }




    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        System.out.println("Testing fragment onAct");
        mTripsList = (RecyclerView) getView().findViewById(R.id.trip_list);
        linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mTripsList.setLayoutManager(linearLayoutManager);
        setupList();
    }
    public void listItemClicked(Trip trip){
        ((MyTripsActivity)getActivity()).setViewPrevTrip(trip);
    }

    public void setList(List<Trip> trips){
        System.out.println("Testing fragment sestList");
        mTrips = trips;
    }





    public void setupList(){
        List<Trip> revTrips;
        if(mTrips != null){
            revTrips = new ArrayList<>(mTrips);
            Collections.reverse(revTrips);
        }else{
            revTrips = new ArrayList<>();
        }
        mTripAdapter = new TripAdapter(getActivity().getApplicationContext(),revTrips,this);
        mTripsList.setAdapter(mTripAdapter);

    }
}

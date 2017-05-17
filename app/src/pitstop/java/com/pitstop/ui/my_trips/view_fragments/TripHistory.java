package com.pitstop.ui.my_trips.view_fragments;

import android.app.Fragment;
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
import com.pitstop.models.Trip;
import com.pitstop.ui.my_trips.MyTripsActivity;

import java.util.List;

/**
 * Created by Matthew on 2017-05-10.
 */

public class TripHistory extends Fragment {
    private FloatingActionButton addTripFab;

    private RecyclerView mTripsList;
    private TripAdapter mTripAdapter;
    private List<Trip> mTrips;

    private LocalTripAdapter localTripAdapter;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_trip_history, container, false);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupList();
        addTripFab = (FloatingActionButton)getView().findViewById(R.id.add_trip_fab);
        addTripFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MyTripsActivity)getActivity()).setViewAddTrip();
            }
        });
    }
    public void listItemClicked(Trip trip){
        ((MyTripsActivity)getActivity()).setViewPrevTrip(trip);
        System.out.println("testing "+trip.getStartAddress());
    }

    public void setupList(){
        localTripAdapter = new LocalTripAdapter(getActivity().getApplicationContext());
        mTrips = localTripAdapter.getAllTrips();
        mTripsList = (RecyclerView) getView().findViewById(R.id.trip_list);
        mTripAdapter = new TripAdapter(getActivity().getApplicationContext(),mTrips,this);
        mTripsList.setAdapter(mTripAdapter);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mTripsList.setLayoutManager(linearLayoutManager);
    }
}

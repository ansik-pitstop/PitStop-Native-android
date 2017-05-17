package com.pitstop.ui.my_trips.view_fragments;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Trip;


import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-16.
 */

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripsViewHolder>  {

    private final int VIEW_TYPE_EMPTY = 100;
    private final int VIEW_TYPE_CUSTOM = 101;

    private final List<Trip> mTrips;
    private final Context mContext;
    private TripHistory tripHistory;

    public TripAdapter(Context context, @NonNull List<Trip> trips, TripHistory mTripHistory) {
        mTrips = trips;
        mContext = context;
        tripHistory = mTripHistory;
    }


    @Override
    public TripAdapter.TripsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TripAdapter.TripsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false));
    }

    @Override
    public void onBindViewHolder(TripAdapter.TripsViewHolder holder, final int position) {// do this last
        int viewType = getItemViewType(position);
        String pretext = "";

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tripHistory.listItemClicked(mTrips.get(position));
            }
        };
        holder.setClickListener(clickListener);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.date.setText("No Trips");
            holder.details.setText("There are currently no trips");
        } else {
            Trip currentTrip = mTrips.get(position);
            holder.date.setText(dateFormat(currentTrip.getStart().getTime()));
            holder.details.setText(mTrips.get(position).getStartAddress()+" - "+mTrips.get(position).getEndAddress());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mTrips.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (mTrips.isEmpty()) return 1;
        return mTrips.size();
    }


    public class TripsViewHolder extends RecyclerView.ViewHolder{
        TextView date;
        TextView details;
        public TripsViewHolder(View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.trip_date);
            details = (TextView) itemView.findViewById(R.id.trip_details);
        }

        public void setClickListener(final View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }

    }

    private String dateFormat(Long date){
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        return newFormat.format(date);
    }

}

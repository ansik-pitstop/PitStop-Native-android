package com.pitstop.ui.my_trips.view_fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Trip;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Matthew on 2017-05-16.
 */

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripsViewHolder>  {

    private final int VIEW_TYPE_EMPTY = 100;
    private final int VIEW_TYPE_CUSTOM = 101;

    private final List<Trip> mTrips;
    private final Context mContext;
    private TripHistory tripHistory;
    private DecimalFormat decimalFormat;

    public TripAdapter(Context context, @NonNull List<Trip> trips, TripHistory mTripHistory) {
        mTrips = trips;
        mContext = context;
        tripHistory = mTripHistory;
    }


    @Override
    public TripAdapter.TripsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        decimalFormat = new DecimalFormat("0.00");
        return new TripAdapter.TripsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false));
    }

    @Override
    public void onBindViewHolder(TripAdapter.TripsViewHolder holder, final int position) {// do this last
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.date.setText("No Trips");
            holder.details.setText("There are currently no trips");
        } else {
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    tripHistory.listItemClicked(mTrips.get(position));
                }
            };
            holder.setClickListener(clickListener);
            Trip currentTrip = mTrips.get(position);
            holder.date.setText(dateFormat(currentTrip.getStart().getTime()));
            holder.details.setText(addressFormat(currentTrip));
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

    private String addressFormat(Trip trip){
        String addressText;
        String[] addOne = trip.getStartAddress().split(",");
        addressText = addOne[0] + " - " + decimalFormat.format(trip.getTotalDistance()/1000) + " km";
        return addressText;
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

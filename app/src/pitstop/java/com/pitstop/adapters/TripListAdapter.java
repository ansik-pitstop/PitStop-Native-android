package com.pitstop.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.trip.list.TripListView;

import java.util.List;

/**
 * Created by David C. on 12/3/18.
 */

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripViewHolder> {

    private final String TAG = CarsAdapter.class.getSimpleName();

    private Context context;
    private List<Trip> tripList;
    private TripListView tripListView;

    private TripViewHolder lastSelectedRow;
    private long selectedTripId = -1;

    public TripListAdapter(Context context, List<Trip> tripList, TripListView tripListView) {
        this.context = context;
        this.tripList = tripList;
        this.tripListView = tripListView;
    }

    @Override
    public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false);
        TripViewHolder tripViewHolder = new TripViewHolder((view));
//        int position = getItemViewType(viewType);
//
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                tripListView.onTripClicked(tripList.get(position));
//                selectedTripId = tripList.get(position).getId();
//            }
//        });

        return tripViewHolder;
    }

    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {

        holder.bind(tripList.get(position));

        holder.itemView.setOnClickListener(v -> { // Row click

            tripListView.onTripRowClicked(tripList.get(position));

            invertColors(lastSelectedRow, false);

            lastSelectedRow = holder;

            invertColors(lastSelectedRow, true);

            selectedTripId = tripList.get(position).getId();

        });

        holder.tripInfoButton.setOnClickListener(v -> { // Info Button click

            tripListView.onTripInfoClicked(tripList.get(position));

        });

    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tripAddress;
        TextView tripLocation;
        ImageView tripInfoButton;

        public TripViewHolder(View itemView) {
            super(itemView);
            this.tripAddress = itemView.findViewById(R.id.trip_address);
            this.tripLocation = itemView.findViewById(R.id.trip_location);
            this.tripInfoButton = itemView.findViewById(R.id.trip_info_button);
        }

        public void bind(Trip trip) {

            setRowText(trip);

            boolean isSelectedTrip = (selectedTripId == trip.getId());

            if (isSelectedTrip) {

                this.tripAddress.setTextColor(Color.WHITE);
                this.tripLocation.setTextColor(Color.WHITE);
                this.tripInfoButton.setImageResource(R.mipmap.ic_launcher);
                this.itemView.setBackgroundColor(context.getResources().getColor(R.color.facebook_blue));

            } else {

                this.tripAddress.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripLocation.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripInfoButton.setImageResource(R.mipmap.ic_launcher);
                this.itemView.setBackgroundColor(Color.WHITE);

            }

        }

        private void setRowText(Trip trip) {

            // Set Address and Location
            tripAddress.setText("Trip ID: " + trip.getTripId());

            tripLocation.setText("VIN: " + trip.getVin());

            //tripAddress.setText(trip.getLocationStart().getStartStreetLocation() + " - " + trip.getLocationEnd().getEndStreetLocation());

            //tripLocation.setText(trip.getLocationStart().getStartCityLocation() + " - " + trip.getLocationEnd().getEndLocation());

        }

    }

    private void invertColors(TripViewHolder holder, boolean isNowSelected) {


        if (!isNowSelected) { // Set the last selected item to the original colors (only if there was a previous selected item)

            if (holder != null) {

                holder.tripAddress.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                holder.tripLocation.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                holder.tripInfoButton.setImageResource(R.mipmap.ic_launcher);
                holder.itemView.setBackgroundColor(Color.WHITE);

            }

        } else { // Set the selected row in the selected colors

            holder.tripAddress.setTextColor(Color.WHITE);
            holder.tripLocation.setTextColor(Color.WHITE);
            holder.tripInfoButton.setImageResource(R.mipmap.ic_launcher);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.facebook_blue));

        }

    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        int position = getItemViewType(viewType);

        tripViewHolder.itemView.setOnClickListener(v -> { // Row click

            tripListView.onTripRowClicked(tripList.get(position));

            // Reset last selected row aspect
            invertColors(lastSelectedRow, false);

            lastSelectedRow = tripViewHolder;

            // Set the current selected row aspect
            invertColors(lastSelectedRow, true);

            selectedTripId = tripList.get(position).getOldId();

        });

        tripViewHolder.tripInfoButton.setOnClickListener(v -> { // Info Button click

            tripViewHolder.itemView.performClick(); // Select the row as the focused one

            tripListView.onTripInfoClicked(tripList.get(position));

        });

        return tripViewHolder;
    }

    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {

        // In case of redraw having an item selected previously, this assures we'll conserve the
        // reference to the selected row
        boolean isSelectedTrip = (selectedTripId == tripList.get(position).getOldId());
        if (isSelectedTrip) {
            lastSelectedRow = holder;
        }

        holder.bind(tripList.get(position));

    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tripAddress;
        TextView tripDate;
        ImageView tripInfoButton;

        public TripViewHolder(View itemView) {
            super(itemView);
            this.tripAddress = itemView.findViewById(R.id.trip_address);
            this.tripDate = itemView.findViewById(R.id.trip_date);
            this.tripInfoButton = itemView.findViewById(R.id.trip_info_button);
        }

        public void bind(Trip trip) {

            setRowText(trip);

            boolean isSelectedTrip = (selectedTripId == trip.getOldId());

            if (isSelectedTrip) {

                this.tripAddress.setTextColor(Color.WHITE);
                this.tripDate.setTextColor(Color.WHITE);
                this.tripInfoButton.setImageResource(R.drawable.ic_info_white);
                this.itemView.setBackgroundColor(context.getResources().getColor(R.color.facebook_blue));

            } else {

                this.tripAddress.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripDate.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripInfoButton.setImageResource(R.drawable.ic_info_black);
                this.itemView.setBackgroundColor(Color.WHITE);

            }

        }

        private void setRowText(Trip trip) {
            String unknown = context.getResources().getString(R.string.unknown);

            String startStreet = (trip.getLocationStart().getStartStreetLocation() != null ? trip.getLocationStart().getStartStreetLocation() : unknown);
            String endStreet = (trip.getLocationEnd().getEndStreetLocation() != null ? trip.getLocationEnd().getEndStreetLocation() : unknown);
            String startCity = (trip.getLocationStart().getStartCityLocation() != null ? trip.getLocationStart().getStartCityLocation() : unknown);
            String endCity = (trip.getLocationEnd().getEndCityLocation() != null ? trip.getLocationEnd().getEndCityLocation() : unknown);
            String startCountry = (trip.getLocationStart().getStartLocation() != null ? trip.getLocationStart().getStartLocation() : unknown);;
            String endCountry = (trip.getLocationEnd().getEndLocation() != null ? trip.getLocationEnd().getEndLocation() : unknown);

            tripAddress.setText(String.format("%s - %s",startStreet,endStreet));

            try{
                Date date = new Date(Long.valueOf(trip.getTimeStart())*1000);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm aa", Locale.CANADA);
                tripDate.setText(simpleDateFormat.format(date));
            }catch(NumberFormatException e){
                e.printStackTrace();
            }

        }

    }

    private void invertColors(TripViewHolder holder, boolean isNowSelected) {

        if (!isNowSelected) { // Set the last selected item to the original colors (only if there was a previous selected item)

            if (holder != null) {

                holder.tripAddress.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                holder.tripDate.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                holder.tripInfoButton.setImageResource(R.drawable.ic_info_black);
                holder.itemView.setBackgroundColor(Color.WHITE);

            }

        } else { // Set the selected row in the selected colors

            holder.tripAddress.setTextColor(Color.WHITE);
            holder.tripDate.setTextColor(Color.WHITE);
            holder.tripInfoButton.setImageResource(R.drawable.ic_info_white);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.facebook_blue));

        }

    }

    public void restartSelectedId() {
        selectedTripId = -1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}

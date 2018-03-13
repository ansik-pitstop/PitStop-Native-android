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
import com.pitstop.ui.my_trips.view_fragments.TripView;

import java.util.List;

/**
 * Created by David C. on 12/3/18.
 */

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripViewHolder> {

    private final String TAG = CarsAdapter.class.getSimpleName();

    private Context context;
    private List<Trip> tripList;
    private TripView tripView;

    private int selectedTripId = -1;

    public TripListAdapter(Context context, List<Trip> tripList, TripView tripView) {
        this.context = context;
        this.tripList = tripList;
        this.tripView = tripView;
    }

    @Override
    public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false);
        TripViewHolder tripViewHolder = new TripViewHolder((view));
        int position = getItemViewType(viewType);

        //view.setOnClickListener(v -> tripView.onTripClicked(tripList.get(position)));

        return tripViewHolder;
    }

    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {

        holder.bind(tripList.get(position));

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
                this.tripInfoButton.setImageResource(R.drawable.chat);
                this.itemView.setBackgroundColor(context.getResources().getColor(R.color.facebook_blue));

            } else {

                this.tripAddress.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripLocation.setTextColor(context.getResources().getColor(R.color.facebook_blue));
                this.tripInfoButton.setImageResource(R.mipmap.ic_launcher);
                this.itemView.setBackgroundColor(Color.WHITE);

            }

//            tripAddress.setText(trip.get);
//
//            boolean isCarCurrent = car.isCurrentCar();
//            Log.d(TAG, car.getModel() + isCarCurrent);
//            carNameView.setText(car.getYear() + " " + car.getMake() + " " + car.getModel());
//            if (isCarCurrent) {
//                carNameView.setTextColor(Color.rgb(43, 131, 226));
//                scanner.setTextColor(Color.rgb(43, 131, 226));
//                dealershipName.setTextColor(Color.rgb(43, 131, 226));
//            } else {
//                carNameView.setTextColor(Color.BLACK);
//                scanner.setTextColor(Color.GRAY);
//                dealershipName.setTextColor(Color.GRAY);
//            }
//            if (car.getScannerId() == null) {
//                scanner.setText("No Paired Device");
//            } else {
//                scanner.setText(car.getScannerId());
//            }
//            if (dealership.getName().equalsIgnoreCase("No Dealership")
//                    || !dealership.getName().equalsIgnoreCase("No Shop")) {
//                dealershipName.setText(dealership.getName());
//            } else {
//                dealershipName.setText("No Associated Shop");
//            }
        }

        private void setRowText(Trip trip) {

            // Set Address and Location2
            tripAddress.setText("Start Address" + " - " + "End Address");

            tripLocation.setText("Start Location2" + " - " + "End Location2");

        }

    }

}

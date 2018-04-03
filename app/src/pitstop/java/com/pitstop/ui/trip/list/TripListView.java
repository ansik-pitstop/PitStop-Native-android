package com.pitstop.ui.trip.list;

import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;
import com.pitstop.ui.trip.TripActivityObservable;

import java.util.List;

/**
 * Created by David C. on 14/3/18.
 */

public interface TripListView extends ErrorHandlingView, LoadingTabView {

    void displayTripList(List<Trip> listTrip);

    void onTripRowClicked(Trip trip);

    void onTripInfoClicked(Trip trip);

    void showToastStillRefreshing();

    boolean hasBeenPopulated();

    void toggleRecordingButton(boolean recording);

    int getSortType();

    TripActivityObservable getTripActivityObservable();

}

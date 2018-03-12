package com.pitstop.ui.trip;

import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;

/**
 * Created by David C. on 10/3/18.
 */

public interface TripListView extends LoadingTabView, ErrorHandlingView {

    void displayTripList(List<Trip> listTrip);
    void displayTripPolylineOnMap(List<Location> listLocation);
    void openTripDetailsView(Trip trip);
    void removeTrip();
    void updateTripList(List<Trip> listTrip);
    void displayErrorMessage(String errorMessage);

}

package com.pitstop.ui.trip;

import com.pitstop.models.trip.LocationPolyline;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;

/**
 * Created by David C. on 10/3/18.
 */

public interface TripsView extends LoadingTabView, ErrorHandlingView {

    //void displayTripList(List<Trip> listTrip);
    //void onTripClicked(Trip trip);
    void noTrips();
    void displayTripPolylineOnMap(List<LocationPolyline> locationPolyline);
    void displayTripDetailsView(Trip trip);
    void removeTrip();
    //void updateTripList(List<Trip> listTrip);
    void displayErrorMessage(String errorMessage);

}

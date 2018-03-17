package com.pitstop.ui.trip;

import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

/**
 * Created by David C. on 10/3/18.
 */

public interface TripsView extends LoadingTabView, ErrorHandlingView {

    //void displayTripList(List<Trip> listTrip);
    //void onTripClicked(Trip trip);
    void noTrips();

    void thereAreTrips();

    void displayTripPolylineOnMap(PolylineOptions polylineOptions);

    void displayTripDetailsView(Trip trip);

    void requestForDataUpdate();

    void removeTrip();

    //void updateTripList(List<Trip> listTrip);
    void displayErrorMessage(String errorMessage);

}

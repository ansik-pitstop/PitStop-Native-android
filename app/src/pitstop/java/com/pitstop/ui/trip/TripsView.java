package com.pitstop.ui.trip;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

/**
 * Created by David C. on 10/3/18.
 */

public interface TripsView extends LoadingTabView, ErrorHandlingView {

    void noTrips();

    void thereAreTrips();

    void displayTripPolylineOnMap(LatLng startCoord, LatLng endCoord, PolylineOptions polylineOptions);

    void displayTripDetailsView(Trip trip);

    void requestForDataUpdate();

    void clearMap();

    void showToastStillRefreshing();

    void displayErrorMessage(String errorMessage);

}

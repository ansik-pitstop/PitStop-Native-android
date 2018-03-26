package com.pitstop.ui.trip.detail;

import com.pitstop.models.trip.Trip;
import com.pitstop.ui.ErrorHandlingView;

/**
 * Created by David C. on 14/3/18.
 */

public interface TripDetailView extends ErrorHandlingView {

    void loadTripData(Trip trip);

    void onCloseView();

}

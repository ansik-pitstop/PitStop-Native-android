package com.pitstop.ui.dashboard;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;
import com.pitstop.ui.NoCarAddedHandlingView;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public interface DashboardView extends ErrorHandlingView, LoadingTabView
        , NoCarAddedHandlingView {
    void displayMileage(double mileage);
    void displayUpdateMileageDialog();

    void displayUpdateMileageError();
    boolean hasBeenPopulated();

    void displayBuyDeviceDialog();

    void startMyTripsActivity();
}

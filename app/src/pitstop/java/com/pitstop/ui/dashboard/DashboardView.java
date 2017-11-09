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
    void displayDefaultDealershipVisuals(Dealership dealership);
    void displayMercedesDealershipVisuals(Dealership dealership);
    void displayCarDetails(Car car);
    void displayMileage(double mileage);
    void displayUpdateMileageDialog();
    void startRequestServiceActivity();
    void startMyAppointmentsActivity();
    void startMyTripsActivity();
    void displayUpdateMileageError();
    boolean hasBeenPopulated();
    void openAlarmsActivity();
    void displayBuyDeviceDialog();
    void noScanner();

    void hideBadge();

    void showBadges(int alarmCount);
}

package com.pitstop.ui.dashboard;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public interface DashboardView extends ErrorHandlingView, LoadingTabView {
    void displayDefaultDealershipVisuals(Dealership dealership);
    void displayMercedesDealershipVisuals(Dealership dealership);
    void displayCarDetails(String )
    void displayMileage(String mileage);
    void displayUpdateMileageDialog();
    void startRequestServiceActivity();
    void startMyAppointmentsActivity();
    void startMyTripsActivity();

}

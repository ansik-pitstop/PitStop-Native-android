package com.pitstop.ui.mainFragments;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

/**
 * Created by Karol Zdebel on 5/9/2017.
 */

public interface MainDashboardCallback{

    void tripData(TripInfoPackage tripInfoPackage);
}

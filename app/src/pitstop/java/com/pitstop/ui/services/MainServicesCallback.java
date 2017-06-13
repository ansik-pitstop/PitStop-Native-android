package com.pitstop.ui.services;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Work on 5/24/2017.
 */

public interface MainServicesCallback {
    void onServiceDone(CarIssue carIssue);
}

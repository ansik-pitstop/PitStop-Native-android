package com.pitstop.ui.main_activity;

import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

public interface MainActivityCallback {
    void prepareAndStartTutorialSequence();

    void startDisplayIssueActivity(Car dashboardCar, CarIssue issue);
}

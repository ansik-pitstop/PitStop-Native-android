package com.pitstop.ui.main_activity;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

public interface MainActivityCallback {
    void prepareAndStartTutorialSequence();

    void startDisplayIssueActivity(CarIssue issue);
}

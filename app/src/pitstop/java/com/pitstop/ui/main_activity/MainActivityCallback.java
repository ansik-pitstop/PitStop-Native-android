package com.pitstop.ui.main_activity;

import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

public interface MainActivityCallback {
    void startDisplayIssueActivity(Integer carId, List<CarIssue> issue, int position);
}

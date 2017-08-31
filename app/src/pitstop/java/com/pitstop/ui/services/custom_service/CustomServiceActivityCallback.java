package com.pitstop.ui.services.custom_service;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Matt on 2017-07-31.
 */

public interface CustomServiceActivityCallback {
    void finishForm(CarIssue issue);
    boolean getHistorical();
}

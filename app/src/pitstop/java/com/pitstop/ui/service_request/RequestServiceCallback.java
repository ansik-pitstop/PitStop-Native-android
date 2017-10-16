package com.pitstop.ui.service_request;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Matthew on 2017-07-11.
 */

public interface RequestServiceCallback {
    void setViewMainForm();
    String checkTentative();
    void finishActivity();
}

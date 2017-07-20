package com.pitstop.ui.service_request.view_fragment.main_from_view;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Matthew on 2017-07-12.
 */

public interface PresenterCallback {
    void onTimeClicked(String time);
    void onIssueClicked(CarIssue issue);
    void onRemoveClicked(CarIssue issue);
}

package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface IssueHolderListener {
    void onServiceClicked(CarIssue carIssue);
    void onServiceDoneClicked(CarIssue carIssue);
    void onTentativeServiceClicked();
}

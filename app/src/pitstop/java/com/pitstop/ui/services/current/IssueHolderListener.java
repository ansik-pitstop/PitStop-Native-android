package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface IssueHolderListener {
    void onServiceClicked(List<CarIssue> carIssues, int position);
    void onServiceDoneClicked(CarIssue carIssue);
    void onTentativeServiceClicked();
}

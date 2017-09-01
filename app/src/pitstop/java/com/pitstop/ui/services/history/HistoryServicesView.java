package com.pitstop.ui.services.history;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public interface HistoryServicesView extends LoadingTabView, ErrorHandlingView{
    void populateDoneServices(List<CarIssue> doneServices);
    boolean hasBeenPopulated();
    void populateEmptyServices();
    void startCustomServiceActivity();
    void addDoneService(CarIssue doneService);
}

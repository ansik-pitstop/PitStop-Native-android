package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface CurrentServicesView extends ErrorHandlingView, LoadingTabView{
    void displayCarIssues(List<CarIssue> carIssues);
    void displayCustomIssues(List<CarIssue> customIssueList);
    void displayStoredEngineIssues(List<CarIssue> storedEngineIssues);
    void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList);
    void displayRecalls(List<CarIssue> displayRecalls);
    void displayCalendar(CarIssue carIssue);
    void startCustomServiceActivity();
    void removeCarIssue(CarIssue issue);
    void addCustomIssue(CarIssue issue);
}

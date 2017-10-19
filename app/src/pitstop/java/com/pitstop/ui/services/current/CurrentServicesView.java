package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;
import com.pitstop.ui.NoCarAddedHandlingView;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface CurrentServicesView extends ErrorHandlingView, LoadingTabView
        , NoCarAddedHandlingView{
    void displayRoutineServices(List<CarIssue> routineServicesList);
    void displayMyServices(List<CarIssue> myServicesList);
    void displayStoredEngineIssues(List<CarIssue> storedEngineIssuesList);
    void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList);
    void displayRecalls(List<CarIssue> displayRecallsList);
    void displayCalendar(CarIssue carIssue);
    void startCustomServiceActivity();
    void removeCarIssue(CarIssue issue);
    void addMyService(CarIssue issue);
    boolean hasBeenPopulated();
    void startDisplayIssueActivity(List<CarIssue> issues, int position);
}

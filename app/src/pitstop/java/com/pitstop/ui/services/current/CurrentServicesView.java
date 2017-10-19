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
    void notifyIssueDataChanged();
    void displayNoServices(boolean visible);
    void showMyServicesView(boolean show);
    void showRoutineServicesView(boolean show);
    void showPotentialEngineIssuesView(boolean show);
    void showStoredEngineIssuesView(boolean show);
    void showRecallsView(boolean show);
    boolean hasBeenPopulated();
    void startDisplayIssueActivity(List<CarIssue> issues, int position);
}

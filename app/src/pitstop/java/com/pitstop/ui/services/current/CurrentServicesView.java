package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;
import com.pitstop.ui.NoCarAddedHandlingView;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface CurrentServicesView extends ErrorHandlingView, LoadingTabView
        , NoCarAddedHandlingView{
    void displayRoutineServices(List<CarIssue> routineServicesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap);
    void displayMyServices(List<CarIssue> myServicesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap);
    void displayStoredEngineIssues(List<CarIssue> storedEngineIssuesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap);
    void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList
            , LinkedHashMap<CarIssue,Boolean> selectionMap);
    void displayRecalls(List<CarIssue> displayRecallsList
            , LinkedHashMap<CarIssue,Boolean> selectionMap);
    void displayBadge(int count);
    void displayCalendar();
    void startCustomServiceActivity();
    void notifyIssueDataChanged();
    void displayNoServices(boolean visible);
    void showMyServicesView(boolean show);
    void showRoutineServicesView(boolean show);
    void showPotentialEngineIssuesView(boolean show);
    void showStoredEngineIssuesView(boolean show);
    void showRecallsView(boolean show);
    void showMoveToHistory(boolean show);
    boolean hasBeenPopulated();
    void startDisplayIssueActivity(List<CarIssue> issues, int position);
}

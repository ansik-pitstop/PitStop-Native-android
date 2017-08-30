package com.pitstop.ui.services.current;

import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public interface CurrentServicesView{
    void displayCarIssues(List<CarIssue> carIssues);
    void displayCustomIssues(List<CarIssue> customIssueList);
    void displayStoredEngineIssues(List<CarIssue> storedEngineIssues);
    void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList);
    void displayRecalls(List<CarIssue> displayRecalls);
    void displayCalendar(CarIssue carIssue);
    void startCustomServiceActivity();
    void removeCarIssue(CarIssue issue);
    void showLoading();
    void hideLoading();
}

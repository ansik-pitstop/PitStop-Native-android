package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.issue.Issue;

import java.util.List;

/**
 * Created by Matt on 2017-08-17.
 */

public interface HealthReportView {
    void setServicesList(List<CarIssue> issues);
    void setRecallList(List<CarIssue> recalls);
    void setEngineList(List<CarIssue> engineList);
    void toggleServiceList();
    void toggleRecallList();
    void toggleEngineList();
    void servicesLoading(boolean show);
    void startIssueDetails(Car car, CarIssue issue);

     List<CarIssue> getIssues();
     List<CarIssue> getRecalls();
}

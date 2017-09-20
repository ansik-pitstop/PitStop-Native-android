package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;

import java.util.List;

/**
 * Created by Matt on 2017-08-17.
 */

public interface HealthReportView {
    void setServicesList(List<Service> services);
    void setRecallList(List<Recall> recalls);
    void setEngineList(List<EngineIssue> engineIssues);
    void toggleServiceList();
    void toggleRecallList();
    void toggleEngineList();
    void servicesLoading(boolean show);
    void startIssueDetails(Car car, CarIssue issue);
    VehicleHealthReport getVehicleHealthReport();
}

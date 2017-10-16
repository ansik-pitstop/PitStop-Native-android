package com.pitstop.ui.vehicle_health_report.show_report.health_report;

import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matt on 2017-08-17.
 */

public interface HealthReportView {

    enum State{ NEEDS_WORK, GOOD, PERFECT }

    void setServicesList(List<Service> services);
    void setRecallList(List<Recall> recalls);
    void setEngineList(List<EngineIssue> engineIssues);
    void toggleServiceList();
    void toggleRecallList();
    void toggleEngineList();
    void servicesLoading(boolean show);
    void startIssueDetails(Car car, ArrayList<CarIssue> issues, int position);
    void setVehicleHealthSummary(State summary);
    VehicleHealthReport getVehicleHealthReport();
}

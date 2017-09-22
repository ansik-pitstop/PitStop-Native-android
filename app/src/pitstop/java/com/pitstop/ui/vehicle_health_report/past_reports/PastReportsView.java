package com.pitstop.ui.vehicle_health_report.past_reports;

import com.pitstop.models.report.VehicleHealthReport;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public interface PastReportsView{
    void displayHealthReports(List<VehicleHealthReport> vehicleHealthReports);
    void displayHealthReport(VehicleHealthReport vehicleHealthReport);
    void notifyReportDataChange();
    void displayNoHealthReports();
    void onReportClicked(VehicleHealthReport vehicleHealthReport);
    void displayError();
    void displayLoading(boolean display);
    List<VehicleHealthReport> getDisplayedReports();

}

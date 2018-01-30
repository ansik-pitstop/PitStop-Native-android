package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.view.View;

import com.pitstop.models.report.EmissionsReport;

/**
 * Created by Matt on 2017-08-17.
 */

public interface EmissionsReportView {

    void toggleCellDetails(View cell);
    EmissionsReport getEmissionsReport();
    void displayEmissionsUnavailable();
    void displayEmissionsReport(EmissionsReport emissionsReport);
    void toggleEmissionsNotReadySteps();
    void toggleEmissionsResults();
}

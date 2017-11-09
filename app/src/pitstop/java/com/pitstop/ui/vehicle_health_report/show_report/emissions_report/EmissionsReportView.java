package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.view.View;

import com.pitstop.models.report.DieselEmissionsReport;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.PetrolEmissionsReport;

/**
 * Created by Matt on 2017-08-17.
 */

public interface EmissionsReportView {

    void toggleCellDetails(View cell);
    EmissionsReport getEmissionsReport();
    void displayEmissionsUnavailable();
    void displayDieselEmissionsReport(DieselEmissionsReport dieselEmissionsReport);
    void displayPetrolEmissionsReport(PetrolEmissionsReport petrolEmissionsReport);
    void toggleEmissionsNotReadySteps();
    void toggleEmissionsResults(boolean petrol);
}

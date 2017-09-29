package com.pitstop.ui.vehicle_health_report.health_report_progress;

import com.pitstop.models.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;

/**
 * Created by Matt on 2017-08-14.
 */

public interface ReportCallback {
    void setReportView(VehicleHealthReport vehicleHealthReport);
    void setReportView(VehicleHealthReport vehicleHealthReport, EmissionsReport emissionsReport);
    void finishActivity();
}

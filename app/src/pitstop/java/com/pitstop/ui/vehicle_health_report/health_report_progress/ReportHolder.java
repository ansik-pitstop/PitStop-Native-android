package com.pitstop.ui.vehicle_health_report.health_report_progress;

import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public interface ReportHolder {
    VehicleHealthReport getVehicleHealthReport();
    EmissionsReport getEmissionsReport();
}

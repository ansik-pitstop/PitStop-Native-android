package com.pitstop.ui.vehicle_health_report.show_report.health_report;

import com.pitstop.models.report.CarHealthItem;

/**
 * Created by Matt on 2017-08-21.
 */

public interface HealthReportPresenterCallback {
    void issueClicked(CarHealthItem issue);
}

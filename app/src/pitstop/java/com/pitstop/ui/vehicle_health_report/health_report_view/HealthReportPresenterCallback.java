package com.pitstop.ui.vehicle_health_report.health_report_view;

import com.pitstop.models.report.CarHealthItem;

/**
 * Created by Matt on 2017-08-21.
 */

public interface HealthReportPresenterCallback {
    void issueClicked(CarHealthItem issue);
}

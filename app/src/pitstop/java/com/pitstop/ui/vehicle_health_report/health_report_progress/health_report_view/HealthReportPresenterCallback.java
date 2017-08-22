package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Matt on 2017-08-21.
 */

public interface HealthReportPresenterCallback {
    void issueClicked(CarIssue issue);
}

package com.pitstop.ui.vehicle_health_report.health_report_progress;

import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Matt on 2017-08-14.
 */

public interface ReportCallback {
    void setReportView(List<CarIssue> issues, List<CarIssue> recalls);
    void finishActivity();
}

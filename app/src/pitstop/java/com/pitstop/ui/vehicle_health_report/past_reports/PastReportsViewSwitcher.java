package com.pitstop.ui.vehicle_health_report.past_reports;

import com.pitstop.models.report.FullReport;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

interface PastReportsViewSwitcher {
    void setPastReportsView();
    void setReportView(FullReport report);
}

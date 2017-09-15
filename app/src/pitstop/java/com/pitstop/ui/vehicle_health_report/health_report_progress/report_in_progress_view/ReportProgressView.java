package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

/**
 * Created by Matt on 2017-08-16.
 */

public interface ReportProgressView {

    void changeStep(String step);
    void setLoading(int progress);
}

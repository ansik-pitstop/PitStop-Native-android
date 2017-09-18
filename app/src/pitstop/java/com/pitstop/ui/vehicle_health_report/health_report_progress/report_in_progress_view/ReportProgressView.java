package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.content.DialogInterface;

/**
 * Created by Matt on 2017-08-16.
 */

public interface ReportProgressView {

    void changeStep(String step);
    void setLoading(int progress);
    void showError(String title, String body, DialogInterface.OnClickListener onOkClicked);
}

package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressPresenter {
    private ReportProgressView view;

    public void subscirbe(ReportProgressView view){
        this.view = view;
    }

    public void changeStep(String step){
        view.changeStep(step);
    }

}

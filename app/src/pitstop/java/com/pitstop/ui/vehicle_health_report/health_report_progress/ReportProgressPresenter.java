package com.pitstop.ui.vehicle_health_report.health_report_progress;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportProgressPresenter {

    private ReportProgressView view;
    private ReportProgressCallback callback;

    public ReportProgressPresenter(ReportProgressCallback callback){
        this.callback = callback;
    }

    public void subscribe(ReportProgressView view){
        this.view = view;
    }

}

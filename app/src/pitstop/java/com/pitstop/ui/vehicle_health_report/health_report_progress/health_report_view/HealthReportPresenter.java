package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportPresenter {

    private HealthReportView view;

    public void subscribe(HealthReportView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }
}

package com.pitstop.ui.vehicle_health_report.health_report_progress;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportPresenter {

    private ReportView view;
    private ReportCallback callback;

    public ReportPresenter(ReportCallback callback){
        this.callback = callback;
    }

    public void subscribe(ReportView view){
        this.view = view;
        if(view == null){return;}
        view.setReportProgressView();
    }

    public void unsubscribe(){
        this.view = null;
    }

}

package com.pitstop.ui.vehicle_health_report.start_report_view;

/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter {

    private StartReportView view;


    public void subscirbe(StartReportView view){
        this.view = view;
    }

    void onSwitchClicked(boolean b){
        if(b){
            view.setModeEmissions();
        }else{
            view.setModeHealthReport();
        }
    }
}

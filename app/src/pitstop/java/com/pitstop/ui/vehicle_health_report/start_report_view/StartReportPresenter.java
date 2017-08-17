package com.pitstop.ui.vehicle_health_report.start_report_view;

/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter {

    private StartReportView view;


    public void subscribe(StartReportView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    void onSwitchClicked(boolean b){
        if(view == null){return;}
        if(b){
            view.setModeEmissions();
        }else{
            view.setModeHealthReport();
        }
    }
}

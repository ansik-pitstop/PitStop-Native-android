package com.pitstop.ui.vehicle_health_report.start_report_view;

import android.util.Log;

/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter {

    private final String TAG = StartReportPresenter.class.getSimpleName();

    private StartReportView view;


    public void subscribe(StartReportView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    void onSwitchClicked(boolean b){
        Log.d(TAG,"onSwitchClicked()");
        if(view == null){return;}
        if(b){
            view.setModeEmissions();
        }else{
            view.setModeHealthReport();
        }
    }
}

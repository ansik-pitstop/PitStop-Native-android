package com.pitstop.ui.vehicle_health_report.health_report_progress;

import android.util.Log;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportPresenter {

    private final String TAG = getClass().getSimpleName();

    private ReportView view;
    private ReportCallback callback;

    public ReportPresenter(ReportCallback callback){
        this.callback = callback;
    }

    public void subscribe(ReportView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
        if(view == null){return;}
        view.setReportProgressView();
    }

    public void unsubscribe(){
        Log.d(TAG,"subscribe()");
        this.view = null;
    }

}

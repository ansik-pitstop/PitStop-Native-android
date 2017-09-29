package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.util.Log;
import android.view.View;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportPresenter {

    private final String TAG = getClass().getSimpleName();
    private EmissionsReportView view;


    public void subscribe(EmissionsReportView view) {
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void onCellClicked(View cell){
        Log.d(TAG,"onCellClicked()");
        view.toggleCellDetails(cell);
    }

    public void loadEmissionsTest(){
        
    }

}

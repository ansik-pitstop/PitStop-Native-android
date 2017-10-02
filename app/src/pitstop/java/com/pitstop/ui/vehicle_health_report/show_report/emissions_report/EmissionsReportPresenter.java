package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.util.Log;
import android.view.View;

import com.pitstop.models.report.EmissionsReport;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportPresenter {

    private final String TAG = getClass().getSimpleName();
    private EmissionsReportView view;
    private MixpanelHelper mixpanelHelper;

    public EmissionsReportPresenter(MixpanelHelper mixpanelHelper){
        this.mixpanelHelper = mixpanelHelper;
    }

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
        if (view != null){
            EmissionsReport er = view.getEmissionsReport();
            if (er != null){
                view.displayEmissionsReport(er);
            }else{
                view.displayEmissionsUnavailable();
            }

        }
    }

    public void onEmissionResultHolderClicked() {
        if (view != null)
            view.displayEmissionsUnavailableDialog();
    }
}

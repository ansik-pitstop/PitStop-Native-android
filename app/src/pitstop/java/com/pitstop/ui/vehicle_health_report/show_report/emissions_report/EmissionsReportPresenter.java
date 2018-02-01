package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.util.Log;
import android.view.View;

import com.pitstop.models.report.EmissionsReport;
import com.pitstop.utils.MixpanelHelper;

import java.util.LinkedHashMap;

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

    void loadEmissionsReport(){
        Log.d(TAG,"loadEmissionsReport()");
        if (view != null){
            EmissionsReport er = view.getEmissionsReport();
            if (er != null){
                if (er.getReason().equalsIgnoreCase("not ready")){
                    view.displayEmissionsNotReady();
                }else{
                    view.displayEmissionsReport(er);
                }
            }else{
                view.displayEmissionsUnavailable();
            }

        }
    }

    public void onEmissionResultHolderClicked() {
        Log.d(TAG,"onEmissionResultHolderClicked()");
        if (view != null && view.getEmissionsReport() != null)
            if (!view.getEmissionsReport().getReason().equalsIgnoreCase("not ready"))
                view.toggleEmissionsResults();
            else
                view.toggleEmissionsNotReadySteps();
    }

    LinkedHashMap<String,String> getSensors(){
        if (view != null && view.getEmissionsReport() != null){
            return view.getEmissionsReport().getSensors();
        }
        return null;
    }
}

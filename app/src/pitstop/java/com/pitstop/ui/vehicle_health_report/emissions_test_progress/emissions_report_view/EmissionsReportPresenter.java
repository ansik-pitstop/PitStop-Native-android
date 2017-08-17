package com.pitstop.ui.vehicle_health_report.emissions_test_progress.emissions_report_view;

import android.view.View;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportPresenter {

    private EmissionsReportView view;


    public void subscirebe(EmissionsReportView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void onCellClicked(View cell){
        view.toggleCellDetails(cell);
    }

}

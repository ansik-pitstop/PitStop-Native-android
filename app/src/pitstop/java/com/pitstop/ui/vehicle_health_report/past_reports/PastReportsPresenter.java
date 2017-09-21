package com.pitstop.ui.vehicle_health_report.past_reports;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsPresenter {

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private PastReportsView view;

    public PastReportsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(PastReportsView view){
        this.view = view;
    }

    void unsubscribe(){
        this.view = null;
    }

    void populateUI(){
        //Get all the reports and call view.displayHealthReports
    }

    void onReportClicked(VehicleHealthReport vehicleHealthReport){

    }

}

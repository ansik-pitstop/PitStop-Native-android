package com.pitstop.ui.vehicle_health_report.past_reports;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetVehicleHealthReportsUseCase;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsPresenter {

    private final String TAG = getClass().getSimpleName();


    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private PastReportsView view;

    public PastReportsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(PastReportsView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    void populateUI(){
        Log.d(TAG,"populateUI()");
        //Get all the reports and call view.displayHealthReports
        useCaseComponent.getGetVehicleHealthReportsUseCase()
                .execute(new GetVehicleHealthReportsUseCase.Callback() {
                    @Override
                    public void onGotVehicleHealthReports(
                            List<VehicleHealthReport> vehicleHealthReports) {

                        Log.d(TAG,"populateUI() reports: "+vehicleHealthReports);
                        view.displayHealthReports(vehicleHealthReports);
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"populateUI() error retrieving reports: "
                                +error.getMessage());
                        view.displayError();
                    }
                });
    }

    void onReportClicked(VehicleHealthReport vehicleHealthReport){
        Log.d(TAG,"onReportClicked() report: "+vehicleHealthReport);
    }

}

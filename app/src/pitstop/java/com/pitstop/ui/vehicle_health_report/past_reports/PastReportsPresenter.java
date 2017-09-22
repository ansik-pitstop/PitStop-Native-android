package com.pitstop.ui.vehicle_health_report.past_reports;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetVehicleHealthReportsUseCase;
import com.pitstop.interactors.other.SortVehicleHealthReportsUseCase;
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
    private List<VehicleHealthReport> savedVehicleHealthReports;

    private boolean populating = false;

    public PastReportsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(PastReportsView view){
        Log.d(TAG,"subscribe() savedVehicleHealthReports == null? "
                +(savedVehicleHealthReports==null));
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    void populateUI(){
        Log.d(TAG,"populateUI()");
        if (view == null || populating) return;
        view.displayLoading(true);
        populating = true;

        if (savedVehicleHealthReports != null){
            populating = false;
            displayHealthReports(savedVehicleHealthReports);
            if (view != null)
                view.displayLoading(false);
            return;
        }

        //Get all the reports and call view.displayHealthReports
        useCaseComponent.getGetVehicleHealthReportsUseCase()
                .execute(new GetVehicleHealthReportsUseCase.Callback() {
                    @Override
                    public void onGotVehicleHealthReports(
                            List<VehicleHealthReport> vehicleHealthReports) {
                        savedVehicleHealthReports = vehicleHealthReports;
                        Log.d(TAG,"populateUI() reports: "+vehicleHealthReports);
                        populating = false;
                        if (view == null) return;
                        displayHealthReports(vehicleHealthReports);
                        view.displayLoading(false);
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"populateUI() error retrieving reports: "
                                +error.getMessage());
                        populating = false;
                        if (view == null) return;
                        view.displayError();
                        view.displayLoading(false);
                    }
                });
    }

    void onReportClicked(VehicleHealthReport vehicleHealthReport){
        Log.d(TAG,"onReportClicked() report: "+vehicleHealthReport);
        if (view != null)
            view.displayHealthReport(vehicleHealthReport);
    }

    private void displayHealthReports(List<VehicleHealthReport> vehicleHealthReports){
        if (view == null) return;
        if (vehicleHealthReports.isEmpty()){
            view.displayNoHealthReports();
        }else{
            view.displayHealthReports(vehicleHealthReports);
        }
    }

    void onSortNewestDateClicked() {
        Log.d(TAG,"onSortNewestDateClicked()");
        if (populating || savedVehicleHealthReports == null) return;

        useCaseComponent.getSortVehicleHealthReportsUseCase().execute(view.getDisplayedReports()
                , SortVehicleHealthReportsUseCase.SortType.DATE_NEW
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortOldestDateClicked() {
        Log.d(TAG,"onSortOldestDateClicked()");
        if (populating || savedVehicleHealthReports == null) return;

        useCaseComponent.getSortVehicleHealthReportsUseCase().execute(view.getDisplayedReports()
                , SortVehicleHealthReportsUseCase.SortType.DATE_OLD
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortEngineIssuesClicked() {
        Log.d(TAG,"onSortEngineIssuesClicked()");
        if (populating || savedVehicleHealthReports == null || view == null) return;

        useCaseComponent.getSortVehicleHealthReportsUseCase().execute(view.getDisplayedReports()
                , SortVehicleHealthReportsUseCase.SortType.ENGINE_ISSUE
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortServicesClicked() {
        Log.d(TAG,"onSortServicesClicked()");
        if (populating || savedVehicleHealthReports == null) return;

        useCaseComponent.getSortVehicleHealthReportsUseCase().execute(view.getDisplayedReports()
                , SortVehicleHealthReportsUseCase.SortType.SERVICE
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortRecallsClicked() {
        Log.d(TAG,"onSortRecallsClicked()");
        if (populating || savedVehicleHealthReports == null) return;

        useCaseComponent.getSortVehicleHealthReportsUseCase().execute(view.getDisplayedReports()
                , SortVehicleHealthReportsUseCase.SortType.RECALL
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }
}

package com.pitstop.ui.vehicle_health_report.show_report.health_report;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportPresenter implements HealthReportPresenterCallback {

    private final String TAG = getClass().getSimpleName();

    private HealthReportView view;
    private UseCaseComponent component;
    private MixpanelHelper mixpanelHelper;
    private Car dashCar;


    public HealthReportPresenter(UseCaseComponent component, MixpanelHelper mixpanelHelper) {
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(HealthReportView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
        if(view == null){return;}
        getDashboardCar();
        setLists();
    }

    private void getDashboardCar(){
        Log.d(TAG,"getDashboardCar()");
        component.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership) {
                dashCar = car;
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError(RequestError error) {

            }
        });
    }

    public void setLists(){
        Log.d(TAG,"setLists()");
        if (view == null) return;

        VehicleHealthReport vehicleHealthReport = view.getVehicleHealthReport();
        view.setServicesList(vehicleHealthReport.getServices());
        view.setRecallList(vehicleHealthReport.getRecalls());
        view.setEngineList(vehicleHealthReport.getEngineIssues());

        if (vehicleHealthReport.getServices().size() > 5
                || vehicleHealthReport.getEngineIssues().size() > 1
                || vehicleHealthReport.getRecalls().size() > 1){

            view.setVehicleHealthSummary(HealthReportView.State.NEEDS_WORK);
        }else if (vehicleHealthReport.getServices().size() == 0){
            view.setVehicleHealthSummary(HealthReportView.State.PERFECT);
        }
        else{
            view.setVehicleHealthSummary(HealthReportView.State.GOOD);
        }
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void serviceButtonClicked(){
        Log.d(TAG,"serviceButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_SERVICE_LIST
                , MixpanelHelper.VIEW_VHR_RESULT);
        view.toggleServiceList();
    }

    public void recallButtonClicked(){
        Log.d(TAG,"recallButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_RECALL_LIST
                , MixpanelHelper.VIEW_VHR_RESULT);
        view.toggleRecallList();
    }

    public void engineButtonClicked(){
        Log.d(TAG,"engineButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_ENGINE_ISSUE_LIST
                , MixpanelHelper.VIEW_VHR_RESULT);
        view.toggleEngineList();
    }

    @Override
    public void issueClicked(CarHealthItem carHealthItem) {
        Log.d(TAG,"issueClicked()");
        if(dashCar == null){return;}
        ArrayList<CarIssue> carIssues = new ArrayList<>();
        if (carHealthItem instanceof EngineIssue){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_ENGINE_ISSUE_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
            for (EngineIssue e: view.getVehicleHealthReport().getEngineIssues()){
                carIssues.add(CarIssue.fromCarHealthItem(e, dashCar.getId()));
            }
            view.startIssueDetails(dashCar
                    , carIssues, view.getVehicleHealthReport().getEngineIssues().indexOf(carHealthItem));
        }
        else if (carHealthItem instanceof Service){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_SERVICE_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
            for (Service s: view.getVehicleHealthReport().getServices()){
                carIssues.add(CarIssue.fromCarHealthItem(s, dashCar.getId()));
            }
            view.startIssueDetails(dashCar
                    , carIssues, view.getVehicleHealthReport().getServices().indexOf(carHealthItem));
        }
        else if (carHealthItem instanceof Recall){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_RECALL_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
            for (Recall r: view.getVehicleHealthReport().getRecalls()){
                carIssues.add(CarIssue.fromCarHealthItem(r, dashCar.getId()));
            }
            view.startIssueDetails(dashCar
                    , carIssues, view.getVehicleHealthReport().getRecalls().indexOf(carHealthItem));
        }


    }
}


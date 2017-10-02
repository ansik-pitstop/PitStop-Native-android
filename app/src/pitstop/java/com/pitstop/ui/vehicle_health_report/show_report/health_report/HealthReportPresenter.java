package com.pitstop.ui.vehicle_health_report.show_report.health_report;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

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
            public void onCarRetrieved(Car car) {
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

            view.setVehicleHealthSummary("Needs Work");
        }else if (vehicleHealthReport.getServices().size() == 0){

            view.setVehicleHealthSummary("Perfect");
        }
        else{
            view.setVehicleHealthSummary("Good");
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
        if (carHealthItem instanceof EngineIssue){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_ENGINE_ISSUE_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
        }
        else if (carHealthItem instanceof Service){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_SERVICE_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
        }
        else if (carHealthItem instanceof Recall){
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_RECALL_ITEM
                    , MixpanelHelper.VIEW_VHR_RESULT);
        }
        if(dashCar == null){return;}

        view.startIssueDetails(dashCar
                ,CarIssue.fromCarHealthItem(carHealthItem,dashCar.getId()));
    }
}


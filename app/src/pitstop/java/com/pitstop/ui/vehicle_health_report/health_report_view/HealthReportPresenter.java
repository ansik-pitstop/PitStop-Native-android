package com.pitstop.ui.vehicle_health_report.health_report_view;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportPresenter implements HealthReportPresenterCallback {

    private final String TAG = getClass().getSimpleName();

    private HealthReportView view;
    private UseCaseComponent component;
    private Car dashCar;


    public HealthReportPresenter(UseCaseComponent component) {
        this.component = component;
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
        VehicleHealthReport vehicleHealthReport = view.getVehicleHealthReport();
        view.setServicesList(vehicleHealthReport.getServices());
        view.setRecallList(vehicleHealthReport.getRecalls());
        view.setEngineList(vehicleHealthReport.getEngineIssues());
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void serviceButtonClicked(){
        Log.d(TAG,"serviceButtonClicked()");
        view.toggleServiceList();
    }

    public void recallButtonClicked(){
        Log.d(TAG,"recallButtonClicked()");
        view.toggleRecallList();
    }

    public void engineButtonClicked(){
        Log.d(TAG,"engineButtonClicked()");
        view.toggleEngineList();
    }

    @Override
    public void issueClicked(CarHealthItem carHealthItem) {
        Log.d(TAG,"issueClicked()");
        if(dashCar == null){return;}

        view.startIssueDetails(dashCar
                ,CarIssue.fromCarHealthItem(carHealthItem,dashCar.getId()));
    }
}


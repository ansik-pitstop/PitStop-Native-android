package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportPresenter implements HealthReportPresenterCallback {

    private HealthReportView view;
    private UseCaseComponent component;
    private Car dashCar;


    public HealthReportPresenter(UseCaseComponent component) {
        this.component = component;
    }

    public void subscribe(HealthReportView view){
        this.view = view;
        if(view == null){return;}
        getCurrentServices();
        getDashboardCar();
    }
    private void getDashboardCar(){
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

    public void unsubscribe(){
        this.view = null;
    }

    public void getCurrentServices(){
        view.servicesLoading(true);
        component.getCurrentServicesUseCase().execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
                List<CarIssue> recalls = new ArrayList<CarIssue>();
                for(CarIssue c: currentServices){
                    if(c.getIssueType().equals(CarIssue.RECALL)){
                        recalls.add(c);
                    }else if(c.getIssueType().equals(CarIssue.DTC)){
                        currentServices.remove(c);
                    }
                }
                currentServices.addAll(customIssues);
                view.setServicesList(currentServices);
                view.setRecallList(recalls);
                view.setEngineList(new ArrayList<CarIssue>());
                view.servicesLoading(false);
            }

            @Override
            public void onError(RequestError error) {
                view.servicesLoading(false);
            }
        });
    }
    public void serviceButtonClicked(){
        view.toggleServiceList();
    }

    public void recallButtonClicked(){
        view.toggleRecallList();
    }

    public void engineButtonClicked(){
        view.toggleEngineList();
    }

    @Override
    public void issueClicked(CarIssue issue) {
        if(dashCar == null){return;}
        view.startIssueDetails(dashCar,issue);
    }
}


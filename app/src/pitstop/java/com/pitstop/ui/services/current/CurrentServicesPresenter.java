package com.pitstop.ui.services.current;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public class CurrentServicesPresenter {

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private boolean updating = false;
    private CurrentServicesView view;

    public CurrentServicesPresenter(UseCaseComponent useCaseComponent) {
        this.useCaseComponent = useCaseComponent;
    }

    public void subscribe(CurrentServicesView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        if (view == null) return;
        view.startCustomServiceActivity();
    }

    public void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (view == null) return;
        if (updating) return;
        else updating = true;
        view.showLoading();

        List<CarIssue> carIssueList = new ArrayList<>();
        List<CarIssue> customIssueList = new ArrayList<>();
        List<CarIssue> engineIssueList = new ArrayList<>();
        List<CarIssue> potentialEngineIssues = new ArrayList<>();
        List<CarIssue> recallList = new ArrayList<>();

        useCaseComponent.getCurrentServicesUseCase().execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
                Log.d(TAG,"getCurrentServicesUseCase.onGotCurrentServices()");

                if (view == null) return;
                for(CarIssue c:currentServices){
                    if(c.getIssueType().equals(CarIssue.DTC)){
                        engineIssueList.add(c);
                    }else if(c.getIssueType().equals(CarIssue.PENDING_DTC)){
                        potentialEngineIssues.add(c);
                    }else if(c.getIssueType().equals(CarIssue.RECALL)){
                        recallList.add(c);
                    }else{
                        carIssueList.add(c);
                    }
                }
                customIssueList.addAll(customIssues);

                view.displayCarIssues(carIssueList);
                view.displayCustomIssues(customIssueList);
                view.displayPotentialEngineIssues(potentialEngineIssues);
                view.displayStoredEngineIssues(engineIssueList);
                view.displayRecalls(recallList);

                view.hideLoading();
                updating = false;
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getCurrentServicesUseCase.onError()");
                //Todo: error handling
                view.hideLoading();
                updating = false;
            }
        });

    }

    public void onServiceDoneDatePicked(CarIssue carIssue, int year, int month, int day){
        Log.d(TAG,"onServiceDoneDatePicked() year: "+year+", month: "+month+", day: "+day);
        carIssue.setYear(year);
        carIssue.setMonth(month);
        carIssue.setDay(day);

        //When the date is set, update issue to done on that date
        useCaseComponent.markServiceDoneUseCase().execute(carIssue, new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone() {
                Log.d(TAG,"markServiceDoneUseCase().onServiceMarkedAsDone()");
                if (view == null) return;
                view.removeCarIssue(carIssue);
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"markServiceDoneUseCase().onError()");
                //Todo: error handling
            }
        });
    }

    public void onServiceMarkedAsDone(CarIssue carIssue){
        Log.d(TAG,"onServiceMarkedAsDone()");
        if (view == null) return;
        view.displayCalendar(carIssue);
    }

}

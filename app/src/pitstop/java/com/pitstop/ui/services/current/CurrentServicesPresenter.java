package com.pitstop.ui.services.current;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

class CurrentServicesPresenter{

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating = false;
    private CurrentServicesView view;

    CurrentServicesPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(CurrentServicesView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    void onServiceClicked(CarIssue issue){
        if (view == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_LIST_ITEM
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        view.startDisplayIssueActivity(issue);
    }

    void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_CREATE_CUSTOM
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (view == null) return;
        view.startCustomServiceActivity();
    }

    void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        onUpdateNeeded();
    }

    void onCustomIssueCreated(CarIssue issue){
        Log.d(TAG,"onCustomIssueCreated()");
        if (issue == null || view == null) return;
        view.addCustomIssue(issue);
    }

    void onRefresh(){
        Log.d(TAG,"onRefresh()");
        mixpanelHelper.trackViewRefreshed(MixpanelHelper.SERVICE_CURRENT_VIEW);
        onUpdateNeeded();
    }

    void onAppStateChanged(){
        onUpdateNeeded();
    }

    void onUpdateNeeded(){
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
                updating = false;
                if (view == null) return;
                view.displayOnlineView();
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
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getCurrentServicesUseCase.onError()");
                updating = false;
                if (view == null) return;

                view.hideLoading();
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    handleOfflineError();
                }
                else{
                    view.displayOnlineView();
                    view.displayUnknownErrorDialog();
                }

            }
        });

    }

    private void handleOfflineError(){
        Log.d(TAG,"handleOfflineError()");
        if (view == null) return;
        else if (view.hasBeenPopulated()){
            view.displayOfflineErrorDialog();
        }else{
            view.displayOfflineView();
        }
    }

    void onServiceDoneDatePicked(CarIssue carIssue, int year, int month, int day){
        Log.d(TAG,"onServiceDoneDatePicked() year: "+year+", month: "+month+", day: "+day);
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_DONE_DATE_PICKED
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (view == null) return;
        carIssue.setYear(year);
        carIssue.setMonth(month);
        carIssue.setDay(day);

        view.showLoading();
        updating = true;
        //When the date is set, update issue to done on that date
        useCaseComponent.markServiceDoneUseCase().execute(carIssue, EVENT_SOURCE
                , new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone() {
                Log.d(TAG,"markServiceDoneUseCase().onServiceMarkedAsDone()");
                updating = false;
                if (view == null) return;
                view.displayOnlineView();
                view.hideLoading();
                view.removeCarIssue(carIssue);
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"markServiceDoneUseCase().onError() error: "+error.getMessage());
                updating = false;
                if (view == null) return;
                view.hideLoading();

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    handleOfflineError();
                }
                else{
                    view.displayOnlineView();
                    view.displayUnknownErrorDialog();
                }
            }
        });
    }

    void onServiceMarkedAsDone(CarIssue carIssue){
        Log.d(TAG,"onServiceMarkedAsDone()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_MARK_DONE
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (view == null || updating) return;
        view.displayCalendar(carIssue);
    }

}

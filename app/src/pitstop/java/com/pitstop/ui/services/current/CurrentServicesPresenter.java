package com.pitstop.ui.services.current;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

class CurrentServicesPresenter extends TabPresenter<CurrentServicesView> {

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY)
    };

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating = false;

    List<CarIssue> routineServicesList = new ArrayList<>();
    List<CarIssue> myServicesList = new ArrayList<>();
    List<CarIssue> storedEngineIssueList = new ArrayList<>();
    List<CarIssue> potentialEngineIssuesList = new ArrayList<>();
    List<CarIssue> recallList = new ArrayList<>();

    CurrentServicesPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void subscribe(CurrentServicesView view) {
        updating = false;
        super.subscribe(view);
    }

    void onAddCarButtonClicked(){
        if (getView() != null)
            getView().startAddCarActivity();
    }

    void onServiceClicked(List<CarIssue> issues, int position){
        if (getView() == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_LIST_ITEM
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        getView().startDisplayIssueActivity(issues, position);
    }

    void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_CREATE_CUSTOM
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (getView() == null) return;
        getView().startCustomServiceActivity();
    }

    void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        onUpdateNeeded();
    }

    void onCustomIssueCreated(CarIssue issue){
        Log.d(TAG,"onCustomIssueCreated()");
        if (issue == null || getView() == null) return;
        myServicesList.add(issue);
        getView().notifyIssueDataChanged();
    }

    void onRefresh(){
        Log.d(TAG,"onRefresh()");

        mixpanelHelper.trackViewRefreshed(MixpanelHelper.SERVICE_UPCOMING_VIEW);
        if (getView() != null && getView().isRefreshing() && updating){
            getView().hideRefreshing();
        }else{
            onUpdateNeeded();
        }

    }

    @Override
    public void onAppStateChanged(){
        Log.d(TAG,"onAppStateChanged()");
        if (getView() != null) onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() { return EVENT_SOURCE; }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (getView() == null) return;
        if (updating) return;
        else updating = true;
        getView().showLoading();

        useCaseComponent.getCurrentServicesUseCase().execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
                Log.d(TAG,"getCurrentServicesUseCase.onGotCurrentServices()");
                updating = false;
                if (getView() == null) return;

                routineServicesList.clear();
                myServicesList.clear();
                potentialEngineIssuesList.clear();
                storedEngineIssueList.clear();
                recallList.clear();

                getView().displayOnlineView();
                if (currentServices.isEmpty() && customIssues.isEmpty()){
                    getView().displayNoServices(true);
                    getView().showMyServicesView(false);
                    getView().showPotentialEngineIssuesView(false);
                    getView().showRecallsView(false);
                    getView().showRoutineServicesView(false);
                    getView().showStoredEngineIssuesView(false);
                }
                else{
                    getView().displayNoServices(false);
                    for(CarIssue c:currentServices){
                        switch (c.getIssueType()) {
                            case CarIssue.DTC:
                                storedEngineIssueList.add(c);
                                break;
                            case CarIssue.PENDING_DTC:
                                potentialEngineIssuesList.add(c);
                                break;
                            case CarIssue.RECALL:
                                recallList.add(c);
                                break;
                            default:
                                routineServicesList.add(c);
                                break;
                        }
                    }
                    myServicesList.addAll(customIssues);

                    if (routineServicesList.isEmpty()){

                    }else{

                    }
                    if (myServicesList.isEmpty()){

                    }else{

                    }

                    getView().displayRoutineServices(routineServicesList);
                    getView().displayMyServices(myServicesList);
                    getView().displayPotentialEngineIssues(potentialEngineIssuesList);
                    getView().displayStoredEngineIssues(storedEngineIssueList);
                    getView().displayRecalls(recallList);
                }

                getView().hideLoading();
            }

            @Override
            public void onNoCarAdded() {
                updating = false;
                if (getView() == null) return;
                getView().hideLoading();
                getView().displayNoCarView();
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getCurrentServicesUseCase.onError()");
                updating = false;
                if (getView() == null) return;

                getView().hideLoading();
                if (error.getError()!=null) {
                    if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                        handleOfflineError();
                    } else {
                        getView().displayUnknownErrorView();
                    }
                }

            }
        });

    }

    void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        onUpdateNeeded();
    }

    private void handleOfflineError(){
        Log.d(TAG,"handleOfflineError()");
        if (getView() == null) return;
        else if (getView().hasBeenPopulated()){
            getView().displayOfflineErrorDialog();
        }else{
            getView().displayOfflineView();
        }
    }

    private void removeCarIssue(CarIssue c){
        switch (c.getIssueType()) {
            case CarIssue.DTC:
                storedEngineIssueList.remove(c);
                break;
            case CarIssue.PENDING_DTC:
                potentialEngineIssuesList.remove(c);
                break;
            case CarIssue.RECALL:
                recallList.remove(c);
                break;
            case CarIssue.SERVICE_USER:
                myServicesList.remove(c);
                break;
            default:
                routineServicesList.remove(c);
                break;
        }
    }

    void onServiceDoneDatePicked(CarIssue carIssue, int year, int month, int day){
        Log.d(TAG,"onServiceDoneDatePicked() year: "+year+", month: "+month+", day: "+day);
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_DONE_DATE_PICKED
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (getView() == null) return;
        carIssue.setYear(year);
        carIssue.setMonth(month);
        carIssue.setDay(day);

        getView().showLoading();
        updating = true;
        //When the date is set, update issue to done on that date
        useCaseComponent.markServiceDoneUseCase().execute(carIssue, EVENT_SOURCE
                , new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone(CarIssue carIssue) {
                Log.d(TAG,"markServiceDoneUseCase().onServiceMarkedAsDone()");
                updating = false;
                if (getView() == null) return;
                getView().displayOnlineView();
                getView().hideLoading();
                removeCarIssue(carIssue);
                getView().notifyIssueDataChanged();
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"markServiceDoneUseCase().onError() error: "+error.getMessage());
                updating = false;
                if (getView() == null) return;
                getView().hideLoading();

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    handleOfflineError();
                }
                else{
                    getView().displayOnlineView();
                    getView().displayUnknownErrorDialog();
                }
            }
        });
    }

    void onServiceMarkedAsDone(CarIssue carIssue){
        Log.d(TAG,"onServiceMarkedAsDone()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_MARK_DONE
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (getView() == null || updating) return;
        getView().displayCalendar(carIssue);
    }

}

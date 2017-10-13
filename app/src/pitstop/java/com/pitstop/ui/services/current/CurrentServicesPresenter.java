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
        getView().addCustomIssue(issue);
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
                if (getView() == null) return;
                getView().displayOnlineView();
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

                getView().displayCarIssues(carIssueList);
                getView().displayCustomIssues(customIssueList);
                getView().displayPotentialEngineIssues(potentialEngineIssues);
                getView().displayStoredEngineIssues(engineIssueList);
                getView().displayRecalls(recallList);

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
                getView().removeCarIssue(carIssue);
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

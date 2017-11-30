package com.pitstop.ui.services.current;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.set.SetServicesDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private LinkedHashMap<CarIssue, Boolean> selectionMap = new LinkedHashMap<>();
    private List<CarIssue> routineServicesList = new ArrayList<>();
    private List<CarIssue> myServicesList = new ArrayList<>();
    private List<CarIssue> storedEngineIssueList = new ArrayList<>();
    private List<CarIssue> potentialEngineIssuesList = new ArrayList<>();
    private List<CarIssue> recallList = new ArrayList<>();

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
        if (myServicesList.isEmpty())
            getView().showMyServicesView(true);
        if (myServicesList.isEmpty() && potentialEngineIssuesList.isEmpty()
                && storedEngineIssueList.isEmpty() && routineServicesList.isEmpty()
                && recallList.isEmpty())
            getView().displayNoServices(false);
        myServicesList.add(issue);
        selectionMap.put(issue,false);
        getView().displayBadge(selectionMap.keySet().size());
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

                getView().displayBadge(currentServices.size() + customIssues.size());

                routineServicesList.clear();
                myServicesList.clear();
                potentialEngineIssuesList.clear();
                storedEngineIssueList.clear();
                recallList.clear();
                selectionMap.clear();

                getView().displayOnlineView();
                getView().displayNoServices(currentServices.isEmpty() && customIssues.isEmpty());
                for(CarIssue c:currentServices){
                    selectionMap.put(c,false);
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
                for (CarIssue c: customIssues){
                    selectionMap.put(c,false);
                    myServicesList.add(c);
                }

                getView().showRoutineServicesView(!routineServicesList.isEmpty());
                getView().showStoredEngineIssuesView(!storedEngineIssueList.isEmpty());
                getView().showPotentialEngineIssuesView(!potentialEngineIssuesList.isEmpty());
                getView().showMyServicesView(!myServicesList.isEmpty());
                getView().showRecallsView(!recallList.isEmpty());

                getView().displayRoutineServices(routineServicesList, selectionMap);
                getView().displayMyServices(myServicesList, selectionMap);
                getView().displayPotentialEngineIssues(potentialEngineIssuesList, selectionMap);
                getView().displayStoredEngineIssues(storedEngineIssueList, selectionMap);
                getView().displayRecalls(recallList, selectionMap);
                getView().showMoveToHistory(selectionMap.values().contains(true));

                getView().hideLoading();
            }

            @Override
            public void onNoCarAdded() {
                updating = false;
                if (getView() == null) return;
                getView().displayBadge(0);
                getView().hideLoading();
                getView().displayNoCarView();
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getCurrentServicesUseCase.onError()");
                updating = false;
                if (getView() == null) return;

                getView().displayBadge(0);
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

    void onServiceDoneDatePicked(int year, int month, int day){
        Log.d(TAG,"onServiceDoneDatePicked() year: "+year+", month: "+month+", day: "+day);
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_DONE_DATE_PICKED
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (getView() == null) return;
        getView().showLoading();

        List<CarIssue> doneCarIssues = new ArrayList<>();
        for (Map.Entry<CarIssue,Boolean> e: selectionMap.entrySet()){
            if (e.getValue()){
                e.getKey().setYear(year);
                e.getKey().setMonth(month);
                e.getKey().setDay(day);
                doneCarIssues.add(e.getKey());
            }
        }
        if (doneCarIssues.isEmpty()) return;

        updating = true;
        List<CarIssue> markedDoneList = new ArrayList<>();

        useCaseComponent.getSetServicesDoneUseCase().execute(doneCarIssues, EVENT_SOURCE, new SetServicesDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone(@NotNull CarIssue carIssue) {
                //This is invoked several times, after each POST succeeds
                Log.d(TAG,"setServicesDoneUseCase.onServiceMarkedAsDone() carIssue: "+carIssue);
                markedDoneList.add(carIssue);
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"setServicesDoneUseCase.onComplete()");
                updating = false;
                if (getView() == null) return;
                for (CarIssue c: markedDoneList){
                    removeCarIssue(c);
                    selectionMap.remove(c);
                }

                getView().displayBadge(selectionMap.keySet().size());
                getView().showRoutineServicesView(!routineServicesList.isEmpty());
                getView().showStoredEngineIssuesView(!storedEngineIssueList.isEmpty());
                getView().showPotentialEngineIssuesView(!potentialEngineIssuesList.isEmpty());
                getView().showMyServicesView(!myServicesList.isEmpty());
                getView().showRecallsView(!recallList.isEmpty());
                getView().displayNoServices(routineServicesList.isEmpty()
                        && storedEngineIssueList.isEmpty() && potentialEngineIssuesList.isEmpty()
                        && myServicesList.isEmpty() && recallList.isEmpty()
                        && routineServicesList.isEmpty());
                getView().displayOnlineView();
                getView().hideLoading();
                getView().showMoveToHistory(false);
                getView().notifyIssueDataChanged();

            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG,"setServicesDoneUseCase.onError() err: "+error);
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

    void onServiceSelected(CarIssue carIssue){
        Log.d(TAG,"onServiceSelected()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_CURRENT_MARK_DONE
                ,MixpanelHelper.SERVICE_CURRENT_VIEW);
        if (getView() == null || updating) return;


        for (Map.Entry<CarIssue,Boolean> e: selectionMap.entrySet()){
            if (e.getKey().equals(carIssue)){
                e.setValue(!e.getValue());
            }
        }
        getView().showMoveToHistory(selectionMap.values().contains(true));
        Log.d(TAG,"selection map after selecting: "+selectionMap.values());
    }

    public void onMoveToHistoryClicked() {
        Log.d(TAG,"onMoveToHistoryClicked()");
        if (getView() == null) return;
        getView().displayCalendar();
    }
}

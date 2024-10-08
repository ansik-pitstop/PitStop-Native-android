package com.pitstop.ui.services.history;

import android.content.Context;
import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalVariables;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public class HistoryServicesPresenter extends TabPresenter<HistoryServicesView>{

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_HISTORY);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_MILEAGE)
    };

    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;
    private Context context;

    private boolean updating = false;

    public HistoryServicesPresenter(MixpanelHelper mixpanelHelper, UseCaseComponent useCaseComponent, Context context) {
        this.mixpanelHelper = mixpanelHelper;
        this.useCaseComponent = useCaseComponent;
        this.context = context;
    }

    void onAddCarButtonClicked(){
        if (getView() != null)
            getView().startAddCarActivity();
    }

    private Integer getMainCarId() {
        return GlobalVariables.Companion.getMainCarId(context);
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (getView() == null || updating) return;
        updating = true;
        getView().showLoading();

        Integer carId = getMainCarId();
        if (carId == null) return;

        useCaseComponent.getDoneServicesUseCase().execute(carId,
                new GetDoneServicesUseCase.Callback() {
            @Override
            public void onGotDoneServices(List<CarIssue> doneServices, boolean isLocal) {
                if (!isLocal) updating = false;
                if (getView() == null) return;

                if (doneServices.isEmpty()){
                    getView().populateEmptyServices();
                }else{
                    getView().populateDoneServices(doneServices);
                }
                getView().displayOnlineView();

                if (!isLocal){
                    getView().hideLoading();
                }
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
                updating = false;
                if (getView() == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (getView().hasBeenPopulated() && getView().isRefreshing()){
                        getView().displayToast(R.string.offline_error);
                    }
                    else if (!getView().hasBeenPopulated()){
                        getView().displayOfflineView();
                    }
                }
                else{
                    if (getView().hasBeenPopulated() && getView().isRefreshing()){
                        getView().displayToast(R.string.unknown_error);
                    }
                    else if (!getView().hasBeenPopulated()){
                        getView().displayUnknownErrorView();
                    }
                }
                getView().hideLoading();
            }
        });

    }

    void onCustomServiceCreated(CarIssue customService){
        if (getView() == null) return;
        getView().addDoneService(customService);
    }

    void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        onUpdateNeeded();
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

    void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SERVICE_HISTORY_CREATE_CUSTOM
                , MixpanelHelper.SERVICE_HISTORY_VIEW);
        if (getView() == null) return;
        getView().startCustomServiceActivity();
    }

    @Override
    public void subscribe(HistoryServicesView view){
        Log.d(TAG,"subscribe()");
        updating = false;
        super.subscribe(view);
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged()");
        if (getView() != null){
            onUpdateNeeded();
        }
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    public void onOfflineTryAgainClicked() {
        Log.d(TAG,"onOfflineTryAgainClicked()");
        onUpdateNeeded();
    }
}

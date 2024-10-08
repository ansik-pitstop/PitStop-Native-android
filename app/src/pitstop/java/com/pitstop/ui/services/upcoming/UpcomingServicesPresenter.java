package com.pitstop.ui.services.upcoming;

import android.content.Context;
import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalVariables;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */

public class UpcomingServicesPresenter extends TabPresenter<UpcomingServicesView> {

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_UPCOMING);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY)
    };

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;
    private Context context;

    public UpcomingServicesPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper, Context context) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
        this.context = context;
    }

    @Override
    public void subscribe(UpcomingServicesView view){
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
        if (getView() != null) onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        onUpdateNeeded();
    }

    void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        onUpdateNeeded();
    }

    void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        if (getView() != null){
            getView().startAddCarActivity();
        }
    }


    Integer getUserId() {
        return GlobalVariables.Companion.getUserId(context);
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (getView() == null || updating) return;
        updating = true;
        getView().showLoading();

        Integer userId = getUserId();
        if (userId == null) return;

        useCaseComponent.getUpcomingServicesUseCase().execute(userId, new GetUpcomingServicesMapUseCase.Callback() {
            @Override
            public void onGotUpcomingServicesMap(Map<Integer, List<UpcomingService>> serviceMap, boolean local) {
                if (!local) updating = false;
                if (getView() == null) return;
                if (!local) getView().hideLoading();
                getView().displayOnlineView();

                if (!serviceMap.isEmpty()){
                    getView().populateUpcomingServices(serviceMap);
                }else{
                    getView().displayNoServices();
                }
            }

            @Override
            public void onNoCarAdded() {
                Log.d(TAG, "noCarAdded()");
                updating = false;
                if (getView() == null) return;
                getView().hideLoading();
                getView().displayNoCarView();
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                if (error!=null && error.getError()!=null){
                    if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                        if (getView().hasBeenPopulated() && getView().isRefreshing()) {
                            getView().displayOfflineErrorDialog();
                        } else if (!getView().hasBeenPopulated()){
                            getView().displayToast(R.string.offline_error);
                        }
                    } else {
                        if (getView().hasBeenPopulated() && getView().isRefreshing()) {
                            getView().displayToast(R.string.unknown_error);
                        } else if (!getView().hasBeenPopulated()){
                            getView().displayUnknownErrorView();
                        }
                    }
                }
                getView().hideLoading();

            }
        });
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

    public void onUpcomingServiceClicked(List<UpcomingService> services, int position) {
        Log.d(TAG, "onUpcomingServiceClicked()");
        ArrayList<UpcomingService> arrayListUpcomingservices = new ArrayList<>();
        for (UpcomingService service: services){
            arrayListUpcomingservices.add(service);
        }
        getView().openIssueDetailsActivity(arrayListUpcomingservices, position);

    }
}

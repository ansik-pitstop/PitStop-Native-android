package com.pitstop.ui.services.upcoming;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */

public class UpcomingServicesPresenter {

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private UpcomingServicesView view;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public UpcomingServicesPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(UpcomingServicesView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        updating = false;
        this.view = null;
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (view == null || updating) return;
        updating = true;

        useCaseComponent.getUpcomingServicesUseCase().execute(new GetUpcomingServicesMapUseCase.Callback() {
            @Override
            public void onGotUpcomingServicesMap(Map<Integer, List<UpcomingService>> serviceMap) {
                updating = false;
                if (view == null) return;

                if (!serviceMap.isEmpty()){
                    view.displayUpcomingServices(serviceMap);
                }else{
                    view.displayNoServices();
                }
                view.hideLoading();
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (view == null) return;
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (view.isEmpty()){
                        view.displayOfflineView();
                    }
                    else{
                        view.displayOfflineErrorDialog();
                    }
                }
                view.hideLoading();
            }
        });
    }

    void onRefresh(){
        onUpdateNeeded();
        Log.d(TAG,"onRefresh()");
    }
}

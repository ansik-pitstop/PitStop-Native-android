package com.pitstop.ui.services.history;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public class HistoryServicesPresenter {

    private final String TAG = getClass().getSimpleName();

    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;

    private HistoryServicesView view;
    private boolean updating = false;

    public HistoryServicesPresenter(MixpanelHelper mixpanelHelper, UseCaseComponent useCaseComponent) {
        this.mixpanelHelper = mixpanelHelper;
        this.useCaseComponent = useCaseComponent;
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (view == null || updating) return;
        updating = true;
        view.showLoading();

        useCaseComponent.getDoneServicesUseCase().execute(
                new GetDoneServicesUseCase.Callback() {
            @Override
            public void onGotDoneServices(List<CarIssue> doneServices) {
                updating = false;
                if (view == null) return;
                view.displayOnlineView();

                if (doneServices.isEmpty()){
                    view.populateEmptyServices();
                }else{
                    view.populateDoneServices(doneServices);
                }

                view.hideLoading();
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (view == null) return;
                view.hideLoading();

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (view.hasBeenPopulated()){
                        view.displayOfflineErrorDialog();
                    }
                    else{
                        view.displayOfflineView();
                    }
                }
                else{
                    view.displayOnlineView();
                    view.displayUnknownErrorDialog();
                }
            }
        });

    }

    void onCustomServiceCreated(CarIssue customService){
        if (view == null) return;
        view.addDoneService(customService);
    }

    void onRefresh(){
        Log.d(TAG,"onRefresh()");
        if (view == null) return;
        onUpdateNeeded();
    }

    void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        if (view == null) return;
        view.startCustomServiceActivity();
    }

    void subscribe(HistoryServicesView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void onOfflineTryAgainClicked() {
        Log.d(TAG,"onOfflineTryAgainClicked()");
        onUpdateNeeded();
    }
}

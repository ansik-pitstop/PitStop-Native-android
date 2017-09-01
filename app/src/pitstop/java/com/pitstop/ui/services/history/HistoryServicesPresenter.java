package com.pitstop.ui.services.history;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetHistoryServicesSortedByDateUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public class HistoryServicesPresenter {

    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;

    private HistoryServicesView view;
    private boolean updating = false;

    public HistoryServicesPresenter(MixpanelHelper mixpanelHelper, UseCaseComponent useCaseComponent) {
        this.mixpanelHelper = mixpanelHelper;
        this.useCaseComponent = useCaseComponent;
    }

    void onUpdateNeeded(){
        if (view == null || updating) return;
        updating = true;
        view.showLoading();

        useCaseComponent.getHistoryServicesSortedByDateUseCase().execute(
                new GetHistoryServicesSortedByDateUseCase.Callback() {
            @Override
            public void onGotDoneServices(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues
                    , ArrayList<String> headers) {
                updating = false;
                if (view == null) return;

                view.displayOnlineView();
                view.populateDoneServices(sortedIssues,headers);
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

    void onRefresh(){
        if (view == null) return;
        onUpdateNeeded();
    }

    void onCustomServiceButtonClicked(){
        if (view == null) return;
        view.startCustomServiceActivity();
    }

    void subscribe(HistoryServicesView view){
        this.view = view;
    }

    void unsubscribe(){
        this.view = null;
    }

    public void onOfflineTryAgainClicked() {
        onUpdateNeeded();
    }
}

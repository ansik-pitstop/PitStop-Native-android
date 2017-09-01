package com.pitstop.ui.services.history;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public class HistoryServicesPresenter {

    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;

    private HistoryServicesView view;

    public HistoryServicesPresenter(MixpanelHelper mixpanelHelper, UseCaseComponent useCaseComponent) {
        this.mixpanelHelper = mixpanelHelper;
        this.useCaseComponent = useCaseComponent;
    }

    void onUpdateNeeded(){
        if (view == null) return;

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

    }
}

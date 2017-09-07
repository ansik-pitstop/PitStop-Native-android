package com.pitstop.ui.dashboard;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class DashboardPresenter extends TabPresenter<DashboardView>{

    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    public DashboardPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void onUpdateNeeded(){
        if (BuildConfig.DEBUG && (dealershipId == 4 || dealershipId == 18)){
            //Mercedes
        } else if (!BuildConfig.DEBUG && dealershipId == 14){
            //Mercedes
        } else {
            //Default
        }
    }

    void onUpdateMileageDialogConfirmClicked(double mileage){

    }

    void onMileageClicked(){
        if (getView() != null)
            getView().displayUpdateMileageDialog();
    }

    void onRefresh(){
        onUpdateNeeded();
    }

    void onMyAppointmentsButtonClicked(){
        if (getView() != null)
            getView().startMyAppointmentsActivity();
    }

    void onServiceRequestButtonClicked(){
        if (getView() != null)
            getView().startRequestServiceActivity();
    }

    void onMyTripsButtonClicked(){
        if (getView() != null)
            getView().startMyTripsActivity();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {

    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }
}

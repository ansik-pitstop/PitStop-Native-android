package com.pitstop.ui.dashboard;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.ui.mainFragments.TabPresenter;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class DashboardPresenter extends TabPresenter<DashboardView>{
    void onUpdateNeeded(){

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
        return null;
    }
}

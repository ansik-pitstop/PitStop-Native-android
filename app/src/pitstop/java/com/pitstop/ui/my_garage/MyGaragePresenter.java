package com.pitstop.ui.my_garage;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.dashboard.DashboardPresenter;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGaragePresenter extends TabPresenter<MyGarageView>{

    private static final String TAG = MyGaragePresenter.class.getSimpleName();

   // public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    public MyGaragePresenter (UseCaseComponent useCaseComponent,
                               MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;

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


    public void onMyAppointmentsClicked() {
        getView().openMyAppointments();

    }

    public void onRequestServiceClicked() {
        getView().openRequestService();
    }
}

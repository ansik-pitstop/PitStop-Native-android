package com.pitstop.ui.dashboard;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class DashboardPresenter extends TabPresenter<DashboardView>{

    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public DashboardPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void onUpdateNeeded(){
        if (updating || getView() == null) return;
        updating = true;
        getView().showLoading();

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                updating = false;
                if (getView() == null) return;
                getView().displayOnlineView();

                if (BuildConfig.DEBUG && (car.getDealership().getId() == 4
                        || car.getDealership().getId() == 18)){
                    getView().displayMercedesDealershipVisuals(car.getDealership());
                } else if (!BuildConfig.DEBUG && car.getDealership().getId() == 14){
                    getView().displayMercedesDealershipVisuals(car.getDealership());
                } else {
                    getView().displayDefaultDealershipVisuals(car.getDealership());
                }

                getView().displayCarDetails(car);

                getView().hideLoading();
            }

            @Override
            public void onNoCarSet() {
                updating = false;
                if (getView() == null) return;
                getView().displayNoCarView();
                getView().hideLoading();
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (getView().hasBeenPopulated()){
                        getView().displayOfflineErrorDialog();
                    }
                    else{
                        getView().displayOfflineView();
                    }
                }
                else{
                    getView().displayOnlineView();
                    getView().displayUnknownErrorDialog();
                }

                getView().hideLoading();
            }
        });

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

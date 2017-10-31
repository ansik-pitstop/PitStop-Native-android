package com.pitstop.ui.dashboard;

import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class DashboardPresenter extends TabPresenter<DashboardView>{

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public DashboardPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        if (getView() != null)
            onUpdateNeeded();
    }

    void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        if (getView() != null)
            getView().startAddCarActivity();
    }

    void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        onUpdateNeeded();
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (updating || getView() == null) return;
        updating = true;
        getView().showLoading();

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership) {
                updating = false;
                if (getView() == null) return;
                getView().displayOnlineView();

                Log.d(TAG, Integer.toString(car.get_id()));

                if (BuildConfig.DEBUG && (dealership.getId() == 4
                        || dealership.getId() == 18)){
                    getView().displayMercedesDealershipVisuals(dealership);
                } else if (!BuildConfig.DEBUG && dealership.getId() == 14){
                    getView().displayMercedesDealershipVisuals(dealership);
                } else {
                    getView().displayDefaultDealershipVisuals(dealership);
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
                if (error.getError()!=null) {
                    if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                        if (getView().hasBeenPopulated()) {
                            getView().displayOfflineErrorDialog();
                        } else {
                            getView().displayOfflineView();
                        }
                    }
                }
                else{
                    getView().displayUnknownErrorView();
                }
                getView().hideLoading();
            }
        });

    }

    void onUpdateMileageDialogConfirmClicked(String mileageText){
        Log.d(TAG,"onUpdateMileageDialogConfirmClicked()");
        if (updating) return;
        updating = true;
        getView().showLoading();

        double mileage;
        try{
            mileage = Double.valueOf(mileageText);
        }catch(NumberFormatException e){
            e.printStackTrace();
            getView().displayUpdateMileageError();
            getView().hideLoading();
            updating = false;
            return;
        }

        if (mileage < 0 || mileage > 3000000){
            getView().hideLoading();
            getView().displayUpdateMileageError();
            updating = false;
            return;
        }

        useCaseComponent.updateCarMileageUseCase().execute(mileage
                , new UpdateCarMileageUseCase.Callback() {

            @Override
            public void onMileageUpdated() {
                updating = false;
                EventBus.getDefault().post(new CarDataChangedEvent(
                        new EventTypeImpl(EventType.EVENT_MILEAGE),EVENT_SOURCE));
                if (getView() == null) return;
                getView().hideLoading();

                try{
                    getView().displayMileage(mileage);
                }catch(NumberFormatException e){
                    e.printStackTrace();
                    getView().displayUnknownErrorDialog();
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

    void onMileageClicked(){
        Log.d(TAG,"onMileageClicked()");
        if (getView() != null)
            getView().displayUpdateMileageDialog();
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

    void onMyAppointmentsButtonClicked(){
        Log.d(TAG,"onMyAppointmentsButtonClicked()");
        if (getView() != null)
            getView().startMyAppointmentsActivity();
    }

    void onServiceRequestButtonClicked(){
        Log.d(TAG,"onServiceRequestButtonClicked()");
        if (getView() != null)
            getView().startRequestServiceActivity();
    }

    void onMyTripsButtonClicked(){
        Log.d(TAG,"onMyTripsButtonClicked()");
        if (getView() != null)
            getView().startMyTripsActivity();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        Log.d(TAG,"getIgnoredEventTypes()");
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged()");
        onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() {
        Log.d(TAG,"getSourceType()");
        return EVENT_SOURCE;
    }
}

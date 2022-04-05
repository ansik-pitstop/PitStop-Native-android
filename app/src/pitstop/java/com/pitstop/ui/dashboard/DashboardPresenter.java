package com.pitstop.ui.dashboard;

import android.util.Log;

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
import com.pitstop.repositories.Repository;
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

    private boolean isDealershipMercedes;
    private boolean updating = false;
    private int numAlarms = 0;
    private int carID = 0;
    private Car car = null;

    private boolean carHasScanner  = false;

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
//        onUpdateNeeded();
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (updating || getView() == null) return;
        if (carID == 0) return;
        updating = true;
        getView().showLoading();

        useCaseComponent.getUserCarUseCase().execute(carID, Repository.DATABASE_TYPE.BOTH, new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                Log.d(TAG, "onCarRetrieved(): " + car.getId());
                if (!isLocal)
                    updating = false;
                if (getView() == null) return;

                if (!isLocal){
                    DashboardPresenter.this.carID = car.getId();
                    DashboardPresenter.this.car = car;
                    carHasScanner = !(car.getScanner() == null);
                 /*   useCaseComponent.getGetAlarmCountUseCase().execute(car.getId()
                            , new GetAlarmCountUseCase.Callback() {
                        @Override
                        public void onAlarmCountGot(int alarmCount) {
                            numAlarms = alarmCount;
                            if (alarmCount == 0){
                                if (getView()==null) return;
                                getView().hideBadge();
                            }
                            else {
                                getView().showBadges(alarmCount);
                            }
                        }
                        @Override
                        public void onError(@NotNull RequestError error) {
                            if (getView() == null )return;
                            getView().hideBadge();
                        }
                    });*/
                }

            /*    getView().displayOnlineView();
                Log.d(TAG, Integer.toString(car.getId()));
                isDealershipMercedes = (dealership.getId() == 4
                        || dealership.getId() == 18);

                getView().displayDefaultDealershipVisuals(dealership);
                if (car.getScannerId()==null || car.getScannerId().equalsIgnoreCase("null")) {
                    getView().noScanner();
                    carHasScanner = false;
                }
                else carHasScanner= true;

                getView().displayCarDetails(car);
                if (!isLocal)
                    getView().hideLoading();*/
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                if (!isLocal){
                    updating = false;
                    carHasScanner = false;
                    if (getView() == null) return;
                    getView().displayNoCarView();
                    getView().hideLoading();
                }
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getUserCar() error: "+error);
                updating = false;
                if (getView() == null) return;
                if (error.getError()!=null) {
                    if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                        if (getView().hasBeenPopulated()) {
                            getView().displayOfflineErrorDialog();
                        } else {
                            getView().displayOfflineView();
                        }
                    }else{
                        if (getView().hasBeenPopulated()) {
                            getView().displayUnknownErrorDialog();
                        } else {
                            getView().displayUnknownErrorView();
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

        useCaseComponent.updateCarMileageUseCase().execute(carID, mileage,EVENT_SOURCE
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

    public void onTotalAlarmsClicked() {
        Log.d(TAG,"onTotalAlarmsClicked()");
        if (updating)return;
        if (getView() == null) return;
        if (carHasScanner){
     /*       getView().openAlarmsActivity();*/
        }
        else {
            getView().displayBuyDeviceDialog();
        }
    }



    public boolean isDealershipMercedes(){
        return this.isDealershipMercedes;
    }

    public void setNumAlarms(int alarms){
        this.numAlarms = alarms;
    }

    public int getNumAlarms(){
        return this.numAlarms;
    }


}

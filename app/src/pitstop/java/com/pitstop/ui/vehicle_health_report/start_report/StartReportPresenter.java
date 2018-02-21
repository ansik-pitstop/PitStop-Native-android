package com.pitstop.ui.vehicle_health_report.start_report;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter extends TabPresenter<StartReportView> implements BluetoothConnectionObserver {

    public static final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_SCAN);

    //Everything but car_id
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_MILEAGE)
            , new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP)
            , new EventTypeImpl(EventType.EVENT_DTC_NEW)
            , new EventTypeImpl(EventType.EVENT_SCANNER)
            , new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY)
            , new EventTypeImpl(EventType.EVENT_SERVICES_NEW)
    };

    private final String TAG = StartReportPresenter.class.getSimpleName();

    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private boolean carAdded = true; //Assume car is added, but check when loading view and set to false if not

    public StartReportPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setBluetoothConnectionObservable(BluetoothConnectionObservable bluetoothConnectionObservable){
        Log.d(TAG,"setBluetoothConnectionObservable() state: "+bluetoothConnectionObservable.getDeviceState());
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        if (getView() != null){
            bluetoothConnectionObservable.subscribe(this);

            if (carAdded){
                String state = bluetoothConnectionObservable.getDeviceState();
                switch(state){
                    case BluetoothConnectionObservable.State.CONNECTED_VERIFIED:
                        getView().changeTitle(R.string.device_connected_action_bar,false);
                        break;
                    case BluetoothConnectionObservable.State.CONNECTED_UNVERIFIED:
                        getView().changeTitle(R.string.verifying_device_action_bar,true);
                        break;
                    case BluetoothConnectionObservable.State.VERIFYING:
                        getView().changeTitle(R.string.verifying_device_action_bar,true);
                        break;
                    case BluetoothConnectionObservable.State.CONNECTING:
                        getView().changeTitle(R.string.connecting_to_device,true);
                        break;
                    case BluetoothConnectionObservable.State.FOUND_DEVICES:
                        getView().changeTitle(R.string.found_devices,true);
                        break;
                    case BluetoothConnectionObservable.State.SEARCHING:
                        getView().changeTitle(R.string.searching_for_device_action_bar,true);
                        break;
                    case BluetoothConnectionObservable.State.DISCONNECTED:
                        getView().changeTitle(R.string.scan_title_no_connection,false);
                        break;
                    default:
                        getView().changeTitle(R.string.device_not_connected_action_bar,false);
                        break;
                }
            }
        }
    }

    @Override
    public void subscribe(StartReportView view){
        Log.d(TAG,"subscribe()");
        super.subscribe(view);
        carAdded = true;
        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.subscribe(this);
        }
    }

    @Override
    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        super.unsubscribe();
        carAdded = true;
        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.unsubscribe(this);
        }
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged()");
        if (getView() != null) loadView();
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    void onSwitchClicked(boolean b){
        Log.d(TAG,"onSwitchClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_TOGGLE
                ,MixpanelHelper.VIEW_VHR_TAB);
        if(getView() == null){return;}
        if(b){
            getView().setModeEmissions();
        }else{
            getView().setModeHealthReport();
        }
    }

    void onBluetoothSearchRequested(){
        Log.d(TAG,"onBluetoothSearchRequested()");
        if (getView() == null) return;
        getView().getBluetoothConnectionObservable().requestDeviceSearch(true,false);
    }

    void startReportButtonClicked(boolean emissions){
        Log.d(TAG,"startReportButtonClicked() emissions ? "+emissions);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_START,MixpanelHelper.VIEW_VHR_TAB);
        if (getView() == null || getView().getBluetoothConnectionObservable() == null) return;

        //Check network connection
        useCaseComponent.getCheckNetworkConnectionUseCase().execute(status -> {
            if (getView() == null) return;
            else if (!status) getView().displayOffline();

            //No car added
            else if (!carAdded) getView().promptAddCar();

            //No bluetooth connection
            else if (!getView().getBluetoothConnectionObservable().getDeviceState()
                    .equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){

                //Ask for search
                if (!getView().getBluetoothConnectionObservable().getDeviceState()
                        .equals(BluetoothConnectionObservable.State.SEARCHING)){
                    getView().promptBluetoothSearch();
                }else{
                    getView().displaySearchInProgress();
                }

            }
            else if (emissions){
                getView().startEmissionsProgressActivity();
            }else{
                getView().startVehicleHealthReportProgressActivity();
            }
        });


    }

    void onAddCarClicked(){
        Log.d(TAG,"onAddCarClicked()");
        if (getView() != null) getView().startAddCar();
    }

    void onShowReportsButtonClicked(boolean emissionMode){
        Log.d(TAG,"onShowReportsButtonClicked() emissionMode: "+emissionMode);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_PAST_REPORTS,MixpanelHelper.VIEW_VHR_TAB);
        if (getView() == null)return;

        if (!carAdded){
            getView().promptAddCar();
        }
        else if (emissionMode){
            //Do nothing yet
        }else{
            getView().startPastReportsActivity();
        }
    }

    void onViewReadyForLoad(){
        Log.d(TAG,"onViewReadyForLoad()");
        if (getView() != null) loadView();
    }

    private void loadView(){
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                if (!isLocal){
                    carAdded = true;
                }
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                if (!isLocal){
                    carAdded = false;
                    if (getView() != null) getView().changeTitle(R.string.scan_title_add_car,false);
                }
            }

            @Override
            public void onError(RequestError error) {
            }
        });
    }

    @Override
    public void onSearchingForDevice() {
        Log.d(TAG,"onSearchingForDevice()");
        if (carAdded && getView() != null) getView().changeTitle(R.string.searching_for_device_action_bar,true);
    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {
        Log.d(TAG,"onDeviceReady() readyDevice: "+readyDevice);
        if (carAdded && getView() != null) getView().changeTitle(R.string.tap_to_begin,false);
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG,"onDeviceDisconnected()");
        if (carAdded && getView() != null) getView().changeTitle(R.string.scan_title_no_connection,false);
    }

    @Override
    public void onDeviceVerifying() {
        Log.d(TAG,"onDeviceVerifying");
        if (carAdded && getView() != null) getView().changeTitle(R.string.verifying_device_action_bar,true);
    }

    @Override
    public void onDeviceSyncing() {
        Log.d(TAG,"onDeviceSyncing");
    }

    @Override
    public void onGotSuportedPIDs(String value) {
        Log.d(TAG,"onGotSupportedPIDs() value: "+value);
    }

    @Override
    public void onConnectingToDevice() {
        Log.d(TAG,"onConnectingToDevice()");
        if (carAdded && getView() != null) getView().changeTitle(R.string.connecting_to_device,true);
    }

    @Override
    public void onFoundDevices() {
        Log.d(TAG,"onFoundDevices()");
        if (carAdded && getView() != null) getView().changeTitle(R.string.found_devices,true);
    }
}

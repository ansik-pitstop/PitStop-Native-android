package com.pitstop.ui.vehicle_health_report.start_report;

import android.util.Log;

import com.continental.rvd.mobile_sdk.BindingQuestion;
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error;
import com.jjoe64.graphview.series.DataPoint;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.repositories.Repository;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;


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
    private boolean carAdded = true; //Assume car is added, but check when loading view and set to false if not
    private int pidPackageNum = 0;
    private long lastPidTime = 0;
    private boolean isPidsSupported = false;


    public StartReportPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setBluetoothConnectionObservable(BluetoothConnectionObservable bluetoothConnectionObservable){
        Log.d(TAG,"setBluetoothConnectionObservable() state: "+bluetoothConnectionObservable.getDeviceState());

        bluetoothConnectionObservable.subscribe(this);
        if (getView() != null && carAdded){
            displayBluetoothState(bluetoothConnectionObservable);
        }
    }

    private void displayBluetoothState(BluetoothConnectionObservable bluetoothConnectionObservable){
        if (getView() == null) return;
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

    @Override
    public void subscribe(StartReportView view){
        Log.d(TAG,"subscribe()");
        super.subscribe(view);
        carAdded = true;
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        Disposable d = view.getBluetoothConnectionObservable().take(1).subscribe((next) -> {
            next.subscribe(StartReportPresenter.this);
            //Get supported pids so we know whether to gray out button or not
            if (next.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
                next.getSupportedPids();
            }
            compositeDisposable.clear();
        });

        compositeDisposable.add(d);
    }

    @Override
    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        carAdded = true;
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        Disposable d = getView().getBluetoothConnectionObservable().take(1).subscribe((next) -> {
            next.unsubscribe(StartReportPresenter.this);
            compositeDisposable.clear();
        });
        compositeDisposable.add(d);
        super.unsubscribe();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged() view null? "+(getView() == null));
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
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        Disposable d = getView().getBluetoothConnectionObservable().take(1).subscribe((next)->{
          next.requestDeviceSearch(true, false, success -> {

          });
            compositeDisposable.clear();
        });
        compositeDisposable.add(d);
    }

    void startReportButtonClicked(boolean emissions){
        Log.d(TAG,"startReportButtonClicked() emissions ? "+emissions);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_START,MixpanelHelper.VIEW_VHR_TAB);
        if (getView() == null || getView().getBluetoothConnectionObservable() == null) return;

        if (!getView().checkPermissions()){
            return;
        }

        //Don't stop the service anywhere because it is stopped inside MainActivity (Tight coupling)
        if (!getView().isBluetoothServiceRunning()){
            getView().startBluetoothService();
        }
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        Disposable d = getView().getBluetoothConnectionObservable().take(1).subscribe((next) -> {
            //Check bluetooth connection
            if (!next.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
                //Ask for search
                if (!next.getDeviceState().equals(BluetoothConnectionObservable.State.SEARCHING)) {
                    getView().promptBluetoothSearch();
                } else {
                    getView().displaySearchInProgress();
                }
            }else{
                //Check network connection
                useCaseComponent.getCheckNetworkConnectionUseCase().execute(status -> {
                    if (getView() == null) return;
                    else if (!status) getView().displayOffline();

                        //No car added
                    else if (!carAdded) getView().promptAddCar();

                    else if (emissions){
                        getView().startEmissionsProgressActivity();
                    }else{
                        getView().startVehicleHealthReportProgressActivity();
                    }
                });
            }
            compositeDisposable.clear();
        });
        compositeDisposable.add(d);
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
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        useCaseComponent.getUserCarUseCase().execute(Repository.DATABASE_TYPE.REMOTE, new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                Log.d(TAG,"onCarRetrieved() car: "+car);
                carAdded = true;
                Disposable d = getView().getBluetoothConnectionObservable().take(1).subscribe((next)->{
                    displayBluetoothState(next);
                    compositeDisposable.clear();
                });
                compositeDisposable.add(d);
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                Log.d(TAG,"onNoCarSet()");
                carAdded = false;
                if (getView() != null) getView().changeTitle(R.string.scan_title_add_car,false);
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"onError() err: "+error);
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
        if (carAdded && getView() != null){
            getView().changeTitle(R.string.tap_to_begin,false);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            Disposable disposable = getView().getBluetoothConnectionObservable().take(1).subscribe( next -> {
                next.getSupportedPids();
                compositeDisposable.clear();
            });
            compositeDisposable.add(disposable);
        }
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
        if (getView() != null)
            getView().setLiveDataButtonEnabled(!value.isEmpty());
        isPidsSupported = !value.isEmpty();
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

    @Override
    public void onGotPid(PidPackage pidPackage) {
        Log.d(TAG,"onGotPid() pidPackage: "+pidPackage);
        if (getView() == null) return;
        if (pidPackage.getPids().size() > 0){
            isPidsSupported = true;
            getView().setLiveDataButtonEnabled(true);
        }
        long currentTime = System.currentTimeMillis();
        //Don't display data more often than every 4 seconds, this is because historical data can stream fast
        if (currentTime - lastPidTime > 4000){
            pidPackageNum++;
            String rpm = pidPackage.getPids().get("210C");
            if (rpm != null){
                try{
                    getView().displaySeriesData("210C"
                            ,new DataPoint(pidPackageNum,Integer.valueOf(rpm,16)));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        lastPidTime = currentTime;
    }

    void onGraphClicked(){
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        Disposable disposable = getView().getBluetoothConnectionObservable().take(1).subscribe((next)->{
            if (next.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
                if (isPidsSupported){
                    getView().startGraphActivity();
                }else{
                    getView().displayLiveDataNotSupportedPrompt();
                }
            }else{
                getView().displayBluetoothConnectionRequirePrompt();
            }
            compositeDisposable.clear();
        });
        compositeDisposable.add(disposable);
    }

    @Override
    public void onBindingRequired() {

    }

    @Override
    public void onBindingQuestionPrompted(@NotNull BindingQuestion question) {

    }

    @Override
    public void onBindingProgress(float progress) {

    }

    @Override
    public void onBindingFinished() {

    }

    @Override
    public void onBindingError(@NotNull Error err) {

    }

    @Override
    public void onFirmwareInstallationRequired() {

    }

    @Override
    public void onFirmwareInstallationProgress(float progress) {

    }

    @Override
    public void onFirmwareInstallationFinished() {

    }

    @Override
    public void onFirmwareInstallationError(@NotNull Error err) {

    }
}

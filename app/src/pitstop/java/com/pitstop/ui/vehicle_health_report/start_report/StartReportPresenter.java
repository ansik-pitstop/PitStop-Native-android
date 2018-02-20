package com.pitstop.ui.vehicle_health_report.start_report;

import android.util.Log;

import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.ReadyDevice;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter implements BluetoothConnectionObserver {

    private final String TAG = StartReportPresenter.class.getSimpleName();

    private StartReportView view;
    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;
    private BluetoothConnectionObservable bluetoothConnectionObservable;

    public StartReportPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setBluetoothConnectionObservable(BluetoothConnectionObservable bluetoothConnectionObservable){
        Log.d(TAG,"setBluetoothConnectionObservable() state: "+bluetoothConnectionObservable.getDeviceState());
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        if (view != null){
            bluetoothConnectionObservable.subscribe(this);
            String state = bluetoothConnectionObservable.getDeviceState();
            switch(state){
                case BluetoothConnectionObservable.State.CONNECTED_VERIFIED:
                    view.changeTitle(R.string.device_connected_action_bar);
                    break;
                case BluetoothConnectionObservable.State.CONNECTED_UNVERIFIED:
                    view.changeTitle(R.string.verifying_device_action_bar);
                    break;
                case BluetoothConnectionObservable.State.VERIFYING:
                    view.changeTitle(R.string.verifying_device_action_bar);
                    break;
                case BluetoothConnectionObservable.State.CONNECTING:
                    view.changeTitle(R.string.connecting_to_device);
                    break;
                case BluetoothConnectionObservable.State.FOUND_DEVICES:
                    view.changeTitle(R.string.found_devices);
                    break;
                case BluetoothConnectionObservable.State.SEARCHING:
                    view.changeTitle(R.string.searching_for_device_action_bar);
                    break;
                case BluetoothConnectionObservable.State.DISCONNECTED:
                    view.changeTitle(R.string.device_not_connected_action_bar);
                    break;
                default:
                    view.changeTitle(R.string.device_not_connected_action_bar);
                    break;
            }
        }
    }

    public void subscribe(StartReportView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.subscribe(this);
        }
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.unsubscribe(this);
        }
    }

    void onSwitchClicked(boolean b){
        Log.d(TAG,"onSwitchClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_TOGGLE
                ,MixpanelHelper.VIEW_VHR_TAB);
        if(view == null){return;}
        if(b){
            view.setModeEmissions();
        }else{
            view.setModeHealthReport();
        }
    }

    void onBluetoothSearchRequested(){
        Log.d(TAG,"onBluetoothSearchRequested()");
        if (view == null) return;
        view.getBluetoothConnectionObservable().requestDeviceSearch(true,false);
    }

    void startReportButtonClicked(boolean emissions){
        Log.d(TAG,"startReportButtonClicked() emissions ? "+emissions);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_START,MixpanelHelper.VIEW_VHR_TAB);
        if (view == null || view.getBluetoothConnectionObservable() == null) return;

        //Check network connection
        useCaseComponent.getCheckNetworkConnectionUseCase().execute(status -> {
            if (view == null) return;
            else if (!status) view.displayOffline();
            //No bluetooth connection
            else if (!view.getBluetoothConnectionObservable().getDeviceState()
                    .equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){

                //Ask for search
                if (!view.getBluetoothConnectionObservable().getDeviceState()
                        .equals(BluetoothConnectionObservable.State.SEARCHING)){
                    view.promptBluetoothSearch();
                }else{
                    view.displaySearchInProgress();
                }

            }
            else if (emissions){
                view.startEmissionsProgressActivity();
            }else{
                view.startVehicleHealthReportProgressActivity();
            }
        });


    }

    void onShowReportsButtonClicked(boolean emissionMode){
        Log.d(TAG,"onShowReportsButtonClicked() emissionMode: "+emissionMode);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_PAST_REPORTS,MixpanelHelper.VIEW_VHR_TAB);
        if (view == null) return;
        if (emissionMode){
            //Do nothing yet
        }else{
            view.startPastReportsActivity();
        }
    }

    @Override
    public void onSearchingForDevice() {
        Log.d(TAG,"onSearchingForDevice()");
        if (view != null) view.changeTitle(R.string.searching_for_device_action_bar);
    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {
        Log.d(TAG,"onDeviceReady() readyDevice: "+readyDevice);
        if (view != null) view.changeTitle(R.string.device_connected_action_bar);
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG,"onDeviceDisconnected()");
        if (view != null) view.changeTitle(R.string.scan_title_no_connection);
    }

    @Override
    public void onDeviceVerifying() {
        Log.d(TAG,"onDeviceVerifying");
        if (view != null) view.changeTitle(R.string.verifying_device_action_bar);
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
        if (view != null) view.changeTitle(R.string.connecting_to_device);
    }

    @Override
    public void onFoundDevices() {
        Log.d(TAG,"onFoundDevices()");
        if (view != null) view.changeTitle(R.string.found_devices);
    }
}

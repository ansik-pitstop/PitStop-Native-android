package com.pitstop.ui.vehicle_health_report.start_report;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportPresenter {

    private final String TAG = StartReportPresenter.class.getSimpleName();

    private StartReportView view;
    private MixpanelHelper mixpanelHelper;
    private UseCaseComponent useCaseComponent;

    public StartReportPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(StartReportView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
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
}

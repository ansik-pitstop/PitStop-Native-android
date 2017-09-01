package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.EmissionsMacroUseCase;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressCallback;

import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressPresenter {

    private InProgressView view;
    private EmissionsProgressCallback callback;

    private boolean readyToStart;

    private final int LAST_CARD = 5;

    private BluetoothConnectionObservable bluetooth;

    private EmissionsMacroUseCase emissionsMacroUseCase;
    private UseCaseComponent component;


    private JSONObject emissionsResuts;


    public InProgressPresenter(EmissionsProgressCallback callback, UseCaseComponent component){
        readyToStart = false;
        this.callback = callback;
        this.component = component;
    }

    public void subscribe(InProgressView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void setBlueTooth(BluetoothConnectionObservable blueTooth){
        this.bluetooth = blueTooth;

    }

    public void onBackPressed(){
        if(view.getCardNumber() == 0 || view == null){return;}
        view.back();
    }
    public void onNextPressed(){
        if(view.getCardNumber() == LAST_CARD || view == null){return;}
        view.next();
        if(view.getCardNumber() == LAST_CARD && !readyToStart){
            setReady();
        }
    }

    public void setReady(){
        if(view == null){return;}
        view.setReady();
        readyToStart = true;
    }

    public void onBigButtonPressed(){
        if(view == null){return;}
        if (readyToStart){
            view.switchToProgress();
            emissionsMacroUseCase = new EmissionsMacroUseCase(component, bluetooth, new EmissionsMacroUseCase.Callback() {
                @Override
                public void onStartPID() {
                    view.changeStep("Getting PIDs");
                }

                @Override
                public void onGotPID() {
                    view.changeStep("Got PIDS");
                }

                @Override
                public void onErrorPID() {
                    view.changeStep("ErrorPID");
                }

                @Override
                public void onStartPost2141() {
                    view.changeStep("Posting 2141");
                }

                @Override
                public void onDonePost2141(JSONObject response) {
                    view.changeStep("Done posting 2141");
                    emissionsResuts = response;

                }

                @Override
                public void onErrorPort2141() {
                    view.changeStep("Error2141");
                }

                @Override
                public void onFinish() {
                    showReport();
                }
            });
            emissionsMacroUseCase.start();
        }else {
            view.bounceCards();
        }
    }
    public void showReport(){
        callback.setViewReport(emissionsResuts);
    }
}

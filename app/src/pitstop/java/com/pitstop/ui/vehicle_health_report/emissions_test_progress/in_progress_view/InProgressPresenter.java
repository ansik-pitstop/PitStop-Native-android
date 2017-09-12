package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.EmissionsMacroUseCase;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressCallback;

import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressPresenter {

    private final String TAG = getClass().getSimpleName();

    private InProgressView view;
    private EmissionsProgressCallback callback;

    private boolean readyToStart;

    private final int LAST_CARD = 5;

    private BluetoothConnectionObservable bluetooth;

    private EmissionsMacroUseCase emissionsMacroUseCase;
    private UseCaseComponent component;


    private JSONObject emissionsResuts;

    private boolean error;


    public InProgressPresenter(EmissionsProgressCallback callback, UseCaseComponent component){
        readyToStart = false;
        this.callback = callback;
        this.component = component;
    }

    public void subscribe(InProgressView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void setBluetooth(BluetoothConnectionObservable blueTooth){
        Log.d(TAG,"setBluetooth()");
        this.bluetooth = blueTooth;

    }

    public void onBackPressed(){
        Log.d(TAG,"onBackPressed()");
        if(view.getCardNumber() == 0 || view == null){return;}
        view.back();
    }
    public void onNextPressed(){
        Log.d(TAG,"onNextPressed()");
        if(view.getCardNumber() == LAST_CARD || view == null){return;}
        view.next();
        if(view.getCardNumber() == LAST_CARD && !readyToStart){
            setReady();
        }
    }

    public void setReady(){
        Log.d(TAG,"setReady()");
        if(view == null){return;}
        view.setReady();
        readyToStart = true;
    }

    public void onBigButtonPressed(){
        Log.d(TAG,"onBigButtonPressed()");
        if(view == null){return;}
        if (readyToStart){
            error = false;
            view.switchToProgress();
            emissionsMacroUseCase = new EmissionsMacroUseCase(component, bluetooth, new EmissionsMacroUseCase.Callback() {
                @Override
                public void onStartPID() {
                    Log.d(TAG,"EmissionsMacroUseCase.onStartPID()");
                    if (view == null) return;
                    view.changeStep("Getting PIDs");
                }

                @Override
                public void onGotPID() {
                    Log.d(TAG,"EmissionsMacroUseCase.onGotPID()");
                    if (view == null) return;
                    view.changeStep("Got PIDs");
                }

                @Override
                public void onErrorPID() {
                    Log.d(TAG,"EmissionsMacroUseCase.onErrorPID()");
                    if (view == null) return;
                    error = true;
                    view.endProgress("Error getting 2141 from device");
                }

                @Override
                public void onStartPost2141() {
                    Log.d(TAG,"EmissionsMacroUseCase.onStartPost2141()");
                    if (view == null) return;
                    view.changeStep("Posting 2141");
                }

                @Override
                public void onDonePost2141(JSONObject response) {
                    Log.d(TAG,"EmissionsMacroUseCase.onDonePost2141()");
                    if (view == null) return;
                    view.changeStep("Done posting 2141");
                    emissionsResuts = response;

                }

                @Override
                public void onErrorPort2141() {
                    Log.d(TAG,"EmissionsMacroUseCase.onErrorPost2141()");
                    if (view == null) return;
                    error = true;
                    view.endProgress("Error decoding 2141");
                }

                @Override
                public void onFinish() {
                    Log.d(TAG,"EmissionsMacroUseCase.onFinish()");
                    if (view == null) return;
                    showReport();
                }
            });
            emissionsMacroUseCase.start();
        }else {
            view.bounceCards();
        }
    }
    public void showReport(){
        Log.d(TAG,"showReport()");
        if(error){return;}
        callback.setViewReport(emissionsResuts);
    }
}

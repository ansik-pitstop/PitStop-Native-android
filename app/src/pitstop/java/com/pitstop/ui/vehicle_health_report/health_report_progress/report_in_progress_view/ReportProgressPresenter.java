package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.os.CountDownTimer;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.VHRMacroUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothDtcObserver;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressPresenter {
    private ReportProgressView view;

    private ReportCallback callback;
    private UseCaseComponent component;

    private List<CarIssue> issueList;
    private List<CarIssue> recallList;

    private BluetoothConnectionObservable bluetooth;

    private CountDownTimer timer;

    private VHRMacroUseCase vhrMacroUseCase;

    public ReportProgressPresenter(ReportCallback callback, UseCaseComponent component){
        this. callback = callback;
        this.component = component;
        vhrMacroUseCase = new VHRMacroUseCase(component, new VHRMacroUseCase.Callback() {

            @Override
            public void onStartGetServices() {
                changeStep("Getting services and recalls");
            }

            @Override
            public void onServicesGot(List<CarIssue> issues, List<CarIssue> recalls) {
                issueList = issues;
                recallList = recalls;
            }

            @Override
            public void onServiceError() {

            }

            @Override
            public void onStartGetDTC() {

            }

            @Override
            public void onGotDTC() {

            }

            @Override
            public void onStartPID() {

            }

            @Override
            public void onGotPID() {

            }

            @Override
            public void onFinish() {
                callback.setReportView(issueList,recallList);
            }
        });
    }



    public void subscribe(ReportProgressView view){
        this.view = view;
        if(this.view == null){return;}
        start();
    }

    public void unsubscribe(){
        this.view = null;
    }

    private void start(){
        vhrMacroUseCase.start();
    }



   public void setBluetooth(BluetoothConnectionObservable bluetooth){
       this.bluetooth = bluetooth;
   }

    public void changeStep(String step){
        view.changeStep(step);
    }

    public void setViewReport(){
        if(issueList == null || recallList == null){return;}
        callback.setReportView(issueList,recallList);
    }


}

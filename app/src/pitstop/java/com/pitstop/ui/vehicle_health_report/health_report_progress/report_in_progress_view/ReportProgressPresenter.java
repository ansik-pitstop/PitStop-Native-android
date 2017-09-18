package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.os.CountDownTimer;
import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.VHRMacroUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportCallback;

import java.util.List;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressPresenter {

    private final String TAG = getClass().getSimpleName();

    private ReportProgressView view;

    private ReportCallback callback;
    private UseCaseComponent component;

    private List<CarIssue> issueList;
    private List<CarIssue> recallList;

    private BluetoothConnectionObservable bluetooth;

    private VHRMacroUseCase vhrMacroUseCase;
    private final int PROGRESS_START_GET_SERVICES = 0;
    private final int PROGRESS_START_GET_DTC = 10;
    private final int PROGRESS_START_GET_PID = 90;
    private final int PROGRESS_FINISH = 100;

    private final CountDownTimer dtcLoadingTimer
            = new CountDownTimer((long)BluetoothConnectionObservable.RETRIEVAL_LEN_DTC*1000, 100) {
        @Override
        public void onTick(long millisUntilFinished) {
            if (view == null) return;
            double dtcLen =(int)BluetoothConnectionObservable.RETRIEVAL_LEN_DTC*1000;
            double dtcRange = (PROGRESS_START_GET_PID-PROGRESS_START_GET_DTC);
            double dtcProgress = PROGRESS_START_GET_PID - ((dtcRange/(double)PROGRESS_FINISH)
                    *((double)millisUntilFinished*100/dtcLen));
            view.setLoading((int)dtcProgress);
            Log.d(TAG,"dtcLoadingTimer.onTick() progress: "+dtcProgress+", millisUntilFinished: "
                    +millisUntilFinished);
        }

        @Override
        public void onFinish() {
            Log.d(TAG,"dtcLoadingTimer.onFinish()");
        }
    };

    public ReportProgressPresenter(ReportCallback callback, UseCaseComponent component){
        this. callback = callback;
        this.component = component;
    }

    public void subscribe(ReportProgressView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    private void start(){
        Log.d(TAG,"start()");
        if(view == null || callback == null){return;}
        vhrMacroUseCase.start();
    }

    public void setBluetooth(BluetoothConnectionObservable bluetooth){
       this.bluetooth = bluetooth;
       vhrMacroUseCase = new VHRMacroUseCase(component,bluetooth, new VHRMacroUseCase.Callback() {

           @Override
           public void onStartGetServices() {
               Log.d(TAG,"VHRMacroUseCase.onStartGetServices()");
               changeStep("Getting services and recalls",PROGRESS_START_GET_SERVICES);
           }

           @Override
           public void onServicesGot(List<CarIssue> issues, List<CarIssue> recalls) {
               Log.d(TAG,"VHRMacrouseCase.onServicesGot()");
               issueList = issues;
               recallList = recalls;
           }

           @Override
           public void onServiceError() {
                Log.d(TAG,"VHRMacrouseCase.onServiceError()");
           }

           @Override
           public void onStartGetDTC() {
               changeStep("Retrieving engine codes",PROGRESS_START_GET_DTC);
               dtcLoadingTimer.start();
               Log.d(TAG,"VHRMacrouseCase.onStartGetDTC()");
           }

           @Override
           public void onGotDTC() {
               dtcLoadingTimer.cancel();
               Log.d(TAG,"VHRMacrouseCase.onGotDTC()");
           }

           @Override
           public void onDTCError() {
               Log.d(TAG,"VHRMacrouseCase.onDTCError()");
           }

           @Override
           public void onStartPID() {
               dtcLoadingTimer.cancel();
               changeStep("Retrieving real time engine data", PROGRESS_START_GET_PID);
               Log.d(TAG,"VHRMacrouseCase.onStartPid()");
           }

           @Override
           public void onGotPID() {
               Log.d(TAG,"VHRMacrouseCase.onGotPid()");
           }

           @Override
           public void onPIDError() {
               Log.d(TAG,"VHRMacrouseCase.onPidError()");
           }

           @Override
           public void onFinish() {
               Log.d(TAG,"VHRMacrouseCase.onFinish()");
               changeStep("Completed", PROGRESS_FINISH);
               setViewReport();
           }
       });
       start();
   }

    private void changeStep(String step, int progress){
        Log.d(TAG,"changeStep() step: "+step);
        if(view == null || callback == null){return;}
        view.changeStep(step);
        view.setLoading(progress);
    }

    private void setViewReport(){
        Log.d(TAG,"setViewReport()");
        if(view == null || callback == null){return;}
        if(issueList == null || recallList == null){return;}
        callback.setReportView(issueList,recallList);
    }

}

package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.VHRMacroUseCase;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportCallback;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressPresenter {

    private final String TAG = getClass().getSimpleName();

    private final int DELAY_SET_VIEW_REPORT = 1500;

    private ReportProgressView view;

    private ReportCallback callback;
    private UseCaseComponent component;

    private VehicleHealthReport vehicleHealthReport;
    private BluetoothConnectionObservable bluetooth;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private VHRMacroUseCase vhrMacroUseCase;

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
           public void onStartGeneratingReport() {
               changeStep("Generating report");
               Log.d(TAG,"VHRMacrouseCase.onStartGeneratingReport()");
           }

           @Override
           public void onFinishGeneratingReport(VehicleHealthReport vehicleHealthReport){
               ReportProgressPresenter.this.vehicleHealthReport = vehicleHealthReport;
               Log.d(TAG,"onFinishGeneratingReport() vehicleHealthReport: "
                       +vehicleHealthReport);
           }

           @Override
           public void onErrorGeneratingReport() {
               handleError("Error","Error generating report"
                       ,(DialogInterface dialog, int which) -> callback.finishActivity());
           }

           @Override
           public void onStartGetDTC() {
               changeStep("Retrieving engine codes");
               Log.d(TAG,"VHRMacrouseCase.onStartGetDTC()");
           }

           @Override
           public void onGotDTC() {
               Log.d(TAG,"VHRMacrouseCase.onGotDTC()");
           }

           @Override
           public void onDTCError() {
               Log.d(TAG,"VHRMacrouseCase.onDTCError()");
               handleError("Error","Error retrieving engine codes"
                       ,(DialogInterface dialog, int which) -> callback.finishActivity());
           }

           @Override
           public void onStartPID() {
               changeStep("Retrieving real time engine data");
               Log.d(TAG,"VHRMacrouseCase.onStartPid()");
           }

           @Override
           public void onGotPID() {
               Log.d(TAG,"VHRMacrouseCase.onGotPid()");
           }

           @Override
           public void onPIDError() {
               Log.d(TAG,"VHRMacrouseCase.onPidError()");
               handleError("Error","Error retrieving real time car data"
                       ,(DialogInterface dialog, int which) -> callback.finishActivity());
           }

           @Override
           public void onFinish(boolean success) {
               Log.d(TAG,"VHRMacrouseCase.onFinish() success? "+success);
               if (success){
                   changeStep("Completed");
                   mainHandler.postDelayed(()
                           -> setViewReport(vehicleHealthReport),DELAY_SET_VIEW_REPORT);
               }
           }

           @Override
           public void onProgressUpdate(int progress) {
               Log.d(TAG,"VHRMacroUseCase.onProgressUpdate() progress: "+progress);
               if (view != null) view.setLoading(progress);
           }
       });
       start();
   }

   void onErrorButtonClicked(){
       if (view != null) callback.finishActivity();
   }

    private void changeStep(String step){
        Log.d(TAG,"changeStep() step: "+step);
        if(view == null || callback == null){return;}
        view.changeStep(step);
    }

    private void handleError(String title, String body, DialogInterface.OnClickListener onOkClicked){
        final int ERR_DELAY_LEN = 1500;
        mainHandler.postDelayed(() -> view.showError(title,body,onOkClicked),ERR_DELAY_LEN);
    }

    private void setViewReport(VehicleHealthReport vehicleHealthReport){
        Log.d(TAG,"setViewReport()");
        if(view == null || callback == null || vehicleHealthReport == null) return;
        callback.setReportView(vehicleHealthReport);
    }

}

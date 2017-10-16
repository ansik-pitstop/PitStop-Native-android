package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.MacroUseCases.VHRMacroUseCase;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportCallback;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matt on 2017-08-16.
 */

public class HealthReportProgressPresenter {

    private final String TAG = getClass().getSimpleName();

    private final int DELAY_SET_VIEW_REPORT = 1500;

    private HealthReportProgressView view;

    private ReportCallback callback;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;
    private VehicleHealthReport vehicleHealthReport;
    private EmissionsReport emissionsReport;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private VHRMacroUseCase vhrMacroUseCase;

    public HealthReportProgressPresenter(ReportCallback callback, UseCaseComponent component
            , MixpanelHelper mixpanelHelper){
        this. callback = callback;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(HealthReportProgressView view){
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
       vhrMacroUseCase = new VHRMacroUseCase(component,bluetooth, new VHRMacroUseCase.Callback() {

           @Override
           public void onStartGeneratingReport() {
               changeStep("Generating report");
               Log.d(TAG,"VHRMacrouseCase.onStartGeneratingReport()");
           }

           @Override
           public void onFinishGeneratingReport(VehicleHealthReport vehicleHealthReport
                , EmissionsReport emissionsReport){
               Log.d(TAG,"onFinishGeneratingReport() vhr: "
                       +vehicleHealthReport+" \n et: "+emissionsReport);
               HealthReportProgressPresenter.this.vehicleHealthReport = vehicleHealthReport;
               HealthReportProgressPresenter.this.emissionsReport = emissionsReport;
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GENERATE_REPORT
                       ,MixpanelHelper.SUCCESS);

           }

           @Override
           public void onErrorGeneratingReport() {
               Log.d(TAG,"onErrorGeneratingReport()");
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GENERATE_REPORT
                       ,MixpanelHelper.FAIL);
               handleError("Error","Error generating report" +
                               ", check your network connection"
                       ,(DialogInterface dialog, int which) -> callback.finishActivity());
           }

           @Override
           public void onStartGetDTC() {
               changeStep("Retrieving engine codes");
               Log.d(TAG,"VHRMacrouseCase.onStartGetDTC()");
           }

           @Override
           public void onGotDTC() {
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GET_DTC
                       ,MixpanelHelper.SUCCESS);
               Log.d(TAG,"VHRMacrouseCase.onGotDTC()");
           }

           @Override
           public void onDTCError() {
               Log.d(TAG,"VHRMacrouseCase.onDTCError()");
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GET_DTC
                       ,MixpanelHelper.FAIL);
               handleError("Error","Error retrieving engine codes" +
                               ", make sure device lights are on before scanning"
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
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GET_PID
                       ,MixpanelHelper.SUCCESS);
           }

           @Override
           public void onPIDError() {
               Log.d(TAG,"VHRMacrouseCase.onPidError()");
               mixpanelHelper.trackVhrProcess(MixpanelHelper.STEP_VHR_GET_PID
                       ,MixpanelHelper.FAIL);
               handleError("Error","Error retrieving real time car data" +
                               ", make sure device lights are on before scanning"
                       ,(DialogInterface dialog, int which) -> callback.finishActivity());
           }

           @Override
           public void onFinish(boolean success) {
               Log.d(TAG,"VHRMacrouseCase.onFinish() success? "+success);
               if (success){
                   changeStep("Completed");
                   mainHandler.postDelayed(()
                           -> setViewReport(vehicleHealthReport, emissionsReport),DELAY_SET_VIEW_REPORT);
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
       mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_VHR_ERR_RETRURN
               ,MixpanelHelper.VIEW_VHR_IN_PROGRESS);
       if (view != null) callback.finishActivity();
   }

    private void changeStep(String step){
        Log.d(TAG,"changeStep() step: "+step);
        if(view == null || callback == null){return;}
        view.changeStep(step);
    }

    private void handleError(String title, String body, DialogInterface.OnClickListener onOkClicked){
        final int ERR_DELAY_LEN = 1500;
        mainHandler.postDelayed(() -> {
            if (view != null){
                view.showError(title,body,onOkClicked);
            }
        },ERR_DELAY_LEN);
    }

    private void setViewReport(VehicleHealthReport vehicleHealthReport
            , EmissionsReport emissionsReport){
        Log.d(TAG,"setViewReport() et: "+emissionsReport+", vhr: "+vehicleHealthReport);
        if(view == null || callback == null || vehicleHealthReport == null) return;
        if (emissionsReport == null){
            callback.setReportView(vehicleHealthReport);
        }else{
            callback.setReportView(vehicleHealthReport, emissionsReport);
        }
    }

}

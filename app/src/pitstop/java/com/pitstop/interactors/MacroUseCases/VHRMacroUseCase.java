package com.pitstop.interactors.MacroUseCases;

import android.os.CountDownTimer;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.Interactor;
import com.pitstop.interactors.add.GenerateReportUseCase;
import com.pitstop.interactors.add.GenerateReportUseCaseImpl;
import com.pitstop.interactors.get.GetDTCUseCase;
import com.pitstop.interactors.get.GetDTCUseCaseImpl;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPIDUseCaseImpl;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Matt on 2017-08-23.
 */

public class VHRMacroUseCase {

    private final String TAG = getClass().getSimpleName();

    public interface Callback{
        void onStartGeneratingReport();
        void onFinishGeneratingReport(VehicleHealthReport vehicleHealthReport
                , EmissionsReport emissionsReport);
        void onErrorGeneratingReport(RequestError error);
        void onStartGetDTC();
        void onGotDTC();
        void onDTCError();
        void onStartPID();
        void onGotPID();
        void onPIDError();
        void onFinish(boolean success);
        void onProgressUpdate(int progress);
    }

    private final int TIME_GENERATE_REPORT = 8;
    private final int TYPE_GENERATE_REPORT = 0;
    private final int TYPE_GET_DTC = 1;
    private final int TYPE_GET_PID = 2;
    private final int TIME_PADDING = 4;
    private final int TIME_PID_PADDING_EXTRA = 4;

    private Callback callback;
    private Queue<Interactor> interactorQueue;
    private Queue<ProgressTimer> progressTimerQueue;
    private BluetoothConnectionObservable bluetooth;
    private VehicleHealthReport vehicleHealthReport;
    private EmissionsReport emissionsReport;

    //Lists for progress timers to communicate results
    private DtcPackage retrievedDtc;
    private PidPackage retrievedPid;

    private boolean success = true;
    private Integer carId;

    public VHRMacroUseCase(Integer carId, UseCaseComponent component, BluetoothConnectionObservable bluetooth, Callback callback){
        this.callback = callback;
        this.bluetooth = bluetooth;

        //Use case queue
        interactorQueue = new LinkedList<>();
        interactorQueue.add(component.getGetDTCUseCase());
        interactorQueue.add(component.getGetPIDUseCase());
        interactorQueue.add(component.getGenerateReportUseCase());

        //Timer queue
        progressTimerQueue = new LinkedList<>();
        progressTimerQueue.add(
                new ProgressTimer(carId, TYPE_GET_DTC
                        , BluetoothConnectionObservable.RETRIEVAL_LEN_DTC+TIME_PADDING));
        progressTimerQueue.add(
                new ProgressTimer(carId, TYPE_GET_PID
                        ,BluetoothConnectionObservable.RETRIEVAL_LEN_ALL_PID
                                +TIME_PADDING+TIME_PID_PADDING_EXTRA));
        progressTimerQueue.add(
                new ProgressTimer(carId, TYPE_GENERATE_REPORT,TIME_GENERATE_REPORT+TIME_PADDING));

    }
    public void start(Integer carId){
        Logger.getInstance().logI(TAG,"Macro use case execution started"
                , DebugMessage.TYPE_USE_CASE);
        next(carId);
    }
    private void next(Integer carId){
        Log.d(TAG,"next()");
        if(interactorQueue.isEmpty()){
            finish();
            return;
        }

        //Start progress timer
        progressTimerQueue.peek().start();

        Interactor current = interactorQueue.peek();
        interactorQueue.remove(current);

        if(current instanceof GetDTCUseCaseImpl){
            callback.onStartGetDTC();
            ((GetDTCUseCaseImpl) current).execute(bluetooth, new GetDTCUseCase.Callback() {
                @Override
                public void onGotDTCs(DtcPackage dtc) {
                    Log.d(TAG,"getDTCUseCase.onGotDTCs() dtc: "+dtc);
                    retrievedDtc = dtc;
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"getDTCUseCase.onError() error: "+error.getMessage());
                    success = false;
                }
            });
        }
        else if(current instanceof GetPIDUseCaseImpl){
            Log.d(TAG, "current is instance of GETPIDUSECASEIMPL");
            callback.onStartPID();
            ((GetPIDUseCaseImpl) current).execute(bluetooth, new GetPIDUseCase.Callback() {
                @Override
                public void onGotPIDs(PidPackage pid) {
                    Log.d(TAG,"getPIDUseCase.onGotPIDs() pid: "+pid);
                    retrievedPid = pid;
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"getPidUseCase.onError() error: "+error.getMessage());
                    success = false;
                }
            });
        }
        else if (current instanceof GenerateReportUseCaseImpl){
            callback.onStartGeneratingReport();
            if (retrievedPid == null || retrievedDtc == null){
                callback.onErrorGeneratingReport(RequestError.getUnknownError());
                if (progressTimerQueue.peek() != null) progressTimerQueue.peek().cancel();
                return;
            }
            ((GenerateReportUseCaseImpl)current).execute(carId, retrievedPid, retrievedDtc
                    , new GenerateReportUseCase.Callback() {
                        @Override
                        public void onReportAddedWithoutEmissions(VehicleHealthReport vehicleHealthReport) {
                            Log.d(TAG,"generateReportUseCase.onReportAddedWithoutEmissions() " +
                                    "vhr: "+vehicleHealthReport);
                            VHRMacroUseCase.this.vehicleHealthReport = vehicleHealthReport;
                        }

                        @Override
                        public void onReportAdded(VehicleHealthReport vehicleHealthReport
                                , EmissionsReport emissionsReport) {
                            VHRMacroUseCase.this.vehicleHealthReport = vehicleHealthReport;
                            VHRMacroUseCase.this.emissionsReport = emissionsReport;
                            Log.d(TAG,"generateReportUseCase.onReportAdded() vhr: "
                                    +vehicleHealthReport+", et: "+emissionsReport);
                        }

                        @Override
                        public void onError(RequestError requestError) {
                            Log.d(TAG,"generateReportUseCase.onError() vhr null!");
                            callback.onErrorGeneratingReport(requestError);
                        }
            });
        }
        else{
            finish();
        }
    }

    private void finish(){
        callback.onFinish(success);
    }

    private class ProgressTimer extends CountDownTimer{

        private final int PROGRESS_START_GET_DTC = 0;
        private final int PROGRESS_FINISH_GET_DTC = 60;
        private final int PROGRESS_START_GET_PID = 60;
        private final int PROGRESS_FINISH_GET_PID = 80;
        private final int PROGRESS_START_GENERATE_REPORT = 80;
        private final int PROGRESS_FINISH_GENERATE_REPORT = 100;
        private final int PROGRESS_FINISH = 100;

        private final String TAG = getClass().getSimpleName();

        private double finishProgress;
        private double startProgress;
        private double useCaseTime;
        private int type;
        private Integer carId;

        ProgressTimer(Integer carId, int type, double useCaseTime){
            super((long)useCaseTime*1000,600);
            this.useCaseTime = useCaseTime;
            this.type = type;
            this.carId = carId;
            switch(type){
                case TYPE_GENERATE_REPORT:
                    this.startProgress = PROGRESS_START_GENERATE_REPORT;
                    this.finishProgress = PROGRESS_FINISH_GENERATE_REPORT;
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GENERATE_REPORT, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    break;
                case TYPE_GET_DTC:
                    this.startProgress = PROGRESS_START_GET_DTC;
                    this.finishProgress = PROGRESS_FINISH_GET_DTC;
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GET_DTC, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    break;
                case TYPE_GET_PID:
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GET_PID, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    this.startProgress = PROGRESS_START_GET_PID;
                    this.finishProgress = PROGRESS_FINISH_GET_PID;
                    break;
                default:
                    Log.d(TAG,"ProgressTimer onCreate() error: unknown type");
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            final double range = finishProgress - startProgress;
            final double totalTimeMillis = useCaseTime * 1000;
            final double millisUntilFinishedDouble = (double)millisUntilFinished;
            final double progress = finishProgress - ((range/PROGRESS_FINISH)
                    * (millisUntilFinishedDouble / totalTimeMillis)* 100);
            Log.d(TAG,"progressTimer.onTick() progress: "+progress);
            callback.onProgressUpdate((int)progress);
        }

        @Override
        public void onFinish() {
            Log.d(TAG,"progressTimer.onFinish() type: "+type);
            callback.onProgressUpdate((int)finishProgress);

            switch(type){
                case TYPE_GENERATE_REPORT:
                    if (retrievedDtc == null || retrievedPid == null || vehicleHealthReport == null){
                        callback.onErrorGeneratingReport(RequestError.getUnknownError());
                        Logger.getInstance().logE(TAG,"Macro use case: error generating report!"
                                , DebugMessage.TYPE_USE_CASE);
                        finish();
                    }
                    else{
                        Logger.getInstance().logI(TAG,"Macro use case: finished generating report"
                                , DebugMessage.TYPE_USE_CASE);
                        callback.onFinishGeneratingReport(vehicleHealthReport, emissionsReport);
                        progressTimerQueue.remove();
                        next(carId);
                    }
                    break;

                case TYPE_GET_DTC:
                    if (retrievedDtc == null){
                        Logger.getInstance().logE(TAG,"Macro use case: error retrieving dtc"
                                , DebugMessage.TYPE_USE_CASE);
                        callback.onDTCError();
                        finish();
                    }
                    else{
                        Logger.getInstance().logI(TAG,"Macro use case: got dtc successfully!"
                                , DebugMessage.TYPE_USE_CASE);
                        callback.onGotDTC();
                        progressTimerQueue.remove();
                        next(carId);
                    }
                    break;

                case TYPE_GET_PID:



                    if (retrievedPid == null){
                        Logger.getInstance().logE(TAG,"Macro use case: error retrieving pids!"
                                , DebugMessage.TYPE_USE_CASE);
                        callback.onPIDError();
                        finish();
                    }
                    else{
                        Logger.getInstance().logI(TAG,"Macro use case: retrieved pids successfully"
                                , DebugMessage.TYPE_USE_CASE);
                        callback.onGotPID();
                        progressTimerQueue.remove();
                        next(carId);
                    }
                    break;
            }
        }
    }
}

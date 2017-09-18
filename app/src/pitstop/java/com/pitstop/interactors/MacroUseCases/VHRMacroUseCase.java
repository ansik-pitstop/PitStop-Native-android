package com.pitstop.interactors.MacroUseCases;

import android.os.CountDownTimer;
import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.Interactor;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.get.GetDTCUseCase;
import com.pitstop.interactors.get.GetDTCUseCaseImpl;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPIDUseCaseImpl;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Matt on 2017-08-23.
 */

public class VHRMacroUseCase {

    private final String TAG = getClass().getSimpleName();

    public interface Callback{
        void onStartGetServices();
        void onServicesGot(List<CarIssue> issues, List<CarIssue> recalls);
        void onServiceError();
        void onStartGetDTC();
        void onGotDTC();
        void onDTCError();
        void onStartPID();
        void onGotPID();
        void onPIDError();
        void onFinish(boolean success);
        void onProgressUpdate(int progress);
    }

    private final int TIME_GET_SERVICES = 6;
    private final int TYPE_GET_SERVICES = 0;
    private final int TYPE_GET_DTC = 1;
    private final int TYPE_GET_PID = 2;
    private final int TIME_PADDING = 2;

    private Callback callback;
    private Queue<Interactor> interactorQueue;
    private Queue<ProgressTimer> progressTimerQueue;
    private UseCaseComponent component;
    private BluetoothConnectionObservable bluetooth;

    //Lists for progress timers to communicate results
    private List<CarIssue> retrievedCurrentServices;
    private List<CarIssue> retrievedRecalls;
    private HashMap<String, Boolean> retrievedDtc;
    private HashMap<String, String> retrievedPid;

    private boolean success = true;

    public VHRMacroUseCase(UseCaseComponent component, BluetoothConnectionObservable bluetooth, Callback callback){
        this.callback = callback;
        this.component = component;
        this.bluetooth = bluetooth;
        interactorQueue = new LinkedList<>();
        interactorQueue.add(component.getCurrentServicesUseCase());
        interactorQueue.add(component.getGetDTCUseCase());
        interactorQueue.add(component.getGetPIDUseCase());
        progressTimerQueue = new LinkedList<>();
        progressTimerQueue.add(new ProgressTimer(TYPE_GET_SERVICES,TIME_GET_SERVICES+TIME_PADDING));
        progressTimerQueue.add(
                new ProgressTimer(TYPE_GET_DTC
                        , BluetoothConnectionObservable.RETRIEVAL_LEN_DTC+TIME_PADDING));
        progressTimerQueue.add(
                new ProgressTimer(TYPE_GET_PID
                        ,BluetoothConnectionObservable.RETRIEVAL_LEN_ALL_PID+TIME_PADDING));
    }
    public void start(){
        next();
    }
    private void next(){
        Log.d(TAG,"next()");
        if(interactorQueue.isEmpty()){
            finish();
            return;
        }

        //Start progress timer
        progressTimerQueue.peek().start();

        Interactor current = interactorQueue.peek();
        interactorQueue.remove(current);

        if(current instanceof GetCurrentServicesUseCaseImpl){
            callback.onStartGetServices();
            ((GetCurrentServicesUseCaseImpl) current).execute(new GetCurrentServicesUseCase.Callback() {
                @Override
                public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
                    Log.d(TAG,"getCurrentServicesUseCase.onGotCurrentServices()");
                    List<CarIssue> recalls = new ArrayList<CarIssue>();
                    for(CarIssue c: currentServices){
                        if(c.getIssueType().equals(CarIssue.RECALL)){
                            recalls.add(c);
                        }else if(c.getIssueType().equals(CarIssue.DTC)){
                            currentServices.remove(c);
                        }
                    }
                    currentServices.addAll(customIssues);
                    retrievedCurrentServices = new ArrayList<>(currentServices);
                    retrievedRecalls = new ArrayList<>(recalls);
                }

                @Override
                public void onNoCarAdded(){
                    Log.d(TAG,"getCurrentServicesUseCase.onNoCarAdded()");
                    success = false;
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"getCurrentServicesUseCase.onError() error: "+error.getMessage());
                    success = false;
                }
            });
        }

        else if(current instanceof GetDTCUseCaseImpl){
            callback.onStartGetDTC();
            ((GetDTCUseCaseImpl) current).execute(bluetooth, new GetDTCUseCase.Callback() {
                @Override
                public void onGotDTCs(HashMap<String, Boolean> dtc) {
                    Log.d(TAG,"getDTCUseCase.onGotDTCs() dtc: "+dtc);
                    retrievedDtc = new HashMap<>(dtc);
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"getDTCUseCase.onError() error: "+error.getMessage());
                    success = false;
                }
            });
        }
        else if(current instanceof GetPIDUseCaseImpl){
            callback.onStartPID();
            ((GetPIDUseCaseImpl) current).execute(bluetooth, new GetPIDUseCase.Callback() {
                @Override
                public void onGotPIDs(HashMap<String, String> pid) {
                    Log.d(TAG,"getPIDUseCase.onGotPIDs() pid: "+pid);
                    retrievedPid = new HashMap<>(pid);
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"getPidUseCase.onError() error: "+error.getMessage());
                    success = false;
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

        private final int PROGRESS_START_GET_SERVICES = 0;
        private final int PROGRESS_START_GET_DTC = 10;
        private final int PROGRESS_START_GET_PID = 90;
        private final int PROGRESS_FINISH = 100;

        private final String TAG = getClass().getSimpleName();

        private double finishProgress;
        private double startProgress;
        private double useCaseTime;
        private int type;

        ProgressTimer(int type, double useCaseTime){
            super((long)useCaseTime*1000,300);
            this.useCaseTime = useCaseTime;
            this.type = type;
            switch(type){
                case TYPE_GET_SERVICES:
                    this.startProgress = PROGRESS_START_GET_SERVICES;
                    this.finishProgress = PROGRESS_START_GET_DTC;
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GET_SERVICES, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    break;
                case TYPE_GET_DTC:
                    this.startProgress = PROGRESS_START_GET_DTC;
                    this.finishProgress = PROGRESS_START_GET_PID;
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GET_DTC, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    break;
                case TYPE_GET_PID:
                    Log.d(TAG,"ProgressTimer onCreate() type: TYPE_GET_PID, useCaseTime: "
                            +useCaseTime +", startProgress: "+startProgress+", finishProgress: "
                            +finishProgress);
                    this.startProgress = PROGRESS_START_GET_PID;
                    this.finishProgress = PROGRESS_FINISH;
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
                case TYPE_GET_SERVICES:
                    Log.d(TAG,"progressTimer.onFinish() type: TYPE_GET_SERVICES, retrieviedRecalls null? "
                            +(retrievedRecalls == null) +", retrievedCurrentServices null? "
                            +(retrievedCurrentServices == null));
                    if (retrievedRecalls == null || retrievedCurrentServices == null){
                        callback.onServiceError();
                        finish();
                    }
                    else{
                        callback.onServicesGot(retrievedCurrentServices,retrievedRecalls);
                        progressTimerQueue.remove();
                        next();
                    }
                    break;
                case TYPE_GET_DTC:
                    Log.d(TAG,"progressTimer.onFinish() type: TYPE_GET_DTC, retrieviedDtc null? "
                            +(retrievedDtc == null));
                    if (retrievedDtc == null){
                        callback.onDTCError();
                        finish();
                    }
                    else{
                        callback.onGotDTC();
                        progressTimerQueue.remove();
                        next();
                    }
                    break;
                case TYPE_GET_PID:
                    Log.d(TAG,"progressTimer().onFinish() type: TYPE_GET_PID, retrievedPid null? "
                            +(retrievedPid == null));
                    if (retrievedPid == null){
                        callback.onPIDError();
                        finish();
                    }
                    else{
                        callback.onGotPID();
                        progressTimerQueue.remove();
                        next();
                    }
                    break;
            }
        }
    }
}

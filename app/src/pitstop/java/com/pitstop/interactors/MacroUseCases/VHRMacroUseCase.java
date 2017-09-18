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
        void onFinish();
        void onProgressUpdate(int progress);
    }

    private final int TIME_GET_SERVICES = 2;
    private final int TYPE_GET_SERVICES = 0;
    private final int TYPE_GET_DTC = 1;
    private final int TYPE_GET_PID = 2;

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

    public VHRMacroUseCase(UseCaseComponent component, BluetoothConnectionObservable bluetooth, Callback callback){
        this.callback = callback;
        this.component = component;
        this.bluetooth = bluetooth;
        interactorQueue = new LinkedList<>();
        interactorQueue.add(component.getCurrentServicesUseCase());
        interactorQueue.add(component.getGetDTCUseCase());
        interactorQueue.add(component.getGetPIDUseCase());
        progressTimerQueue = new LinkedList<>();
        progressTimerQueue.add(new ProgressTimer(TYPE_GET_SERVICES,TIME_GET_SERVICES));
        progressTimerQueue.add(
                new ProgressTimer(TYPE_GET_DTC, BluetoothConnectionObservable.RETRIEVAL_LEN_DTC));
        progressTimerQueue.add(
                new ProgressTimer(TYPE_GET_PID,BluetoothConnectionObservable.RETRIEVAL_LEN_ALL_PID));
    }
    public void start(){
        next();
    }
    private void next(){
        if(interactorQueue.isEmpty()){finish();}

        //Start progress timer
        progressTimerQueue.peek().start();

        Interactor current = interactorQueue.peek();
        interactorQueue.remove(current);

        if(current instanceof GetCurrentServicesUseCaseImpl){
            callback.onStartGetServices();
            ((GetCurrentServicesUseCaseImpl) current).execute(new GetCurrentServicesUseCase.Callback() {
                @Override
                public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
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
//                    callback.onServicesGot(currentServices,recalls);
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    next();
                }

                @Override
                public void onNoCarAdded(){
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    callback.onServiceError();
                }

                @Override
                public void onError(RequestError error) {
//                    callback.onServiceError();
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    finish();
                }
            });
        }

        else if(current instanceof GetDTCUseCaseImpl){
            callback.onStartGetDTC();
            ((GetDTCUseCaseImpl) current).execute(bluetooth, new GetDTCUseCase.Callback() {
                @Override
                public void onGotDTCs(HashMap<String, Boolean> dtc) {
                    retrievedDtc = new HashMap<>(dtc);
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    callback.onGotDTC();
//                    next();
                }

                @Override
                public void onError(RequestError error) {
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    callback.onDTCError();
//                    finish();
                }
            });
        }
        else if(current instanceof GetPIDUseCaseImpl){
            callback.onStartPID();
            ((GetPIDUseCaseImpl) current).execute(bluetooth, new GetPIDUseCase.Callback() {
                @Override
                public void onGotPIDs(HashMap<String, String> pid) {
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    callback.onGotPID();
                    retrievedPid = new HashMap<>(pid);
//                    next();
                }

                @Override
                public void onError(RequestError error) {
//                    progressTimerQueue.peek().cancel();
//                    progressTimerQueue.remove();
//                    callback.onPIDError();
//                    finish();
                }
            });
        }
        else{
            finish();
        }
    }

    private void finish(){
        callback.onFinish();
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

        public ProgressTimer(int type, double useCaseTime){
            super((long)useCaseTime*1000,100);
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
            final double progress = finishProgress - ((range/finishProgress)
                    * (millisUntilFinishedDouble / totalTimeMillis)* 100);
            Log.d(TAG,"progressTimer.onTick() progress: "+progress);
            callback.onProgressUpdate((int)progress);
        }

        @Override
        public void onFinish() {
            Log.d(TAG,"progressTimer.onFinish() type: "+type);
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
                        next();
                    }
                    break;
            }
        }
    }
}

package com.pitstop.interactors.MacroUseCases;

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
    }

    private Callback callback;

    public Queue<Interactor> interactorQueue;

    private UseCaseComponent component;

    private BluetoothConnectionObservable bluetooth;



    public VHRMacroUseCase(UseCaseComponent component, BluetoothConnectionObservable bluetooth, Callback callback){
        this.callback = callback;
        this.component = component;
        this.bluetooth = bluetooth;
        interactorQueue = new LinkedList<Interactor>();
        interactorQueue.add(component.getCurrentServicesUseCase());
        interactorQueue.add(component.getGetDTCUseCase());
        interactorQueue.add(component.getGetPIDUseCase());
    }
    public void start(){
        next();
    }
    private void next(){
        if(interactorQueue.isEmpty()){finish();}
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
                    callback.onServicesGot(currentServices,recalls);
                    next();
                }

                @Override
                public void onError(RequestError error) {
                    callback.onServiceError();
                    finish();
                }
            });
        }

        else if(current instanceof GetDTCUseCaseImpl){
            callback.onStartGetDTC();
            ((GetDTCUseCaseImpl) current).execute(bluetooth, new GetDTCUseCase.Callback() {
                @Override
                public void onGotDTCs(HashMap<String, Boolean> dtc) {
                    callback.onGotDTC();
                    next();
                }

                @Override
                public void onError(RequestError error) {
                    callback.onDTCError();
                    finish();
                }
            });
        }
        else if(current instanceof GetPIDUseCaseImpl){
            callback.onStartPID();
            ((GetPIDUseCaseImpl) current).execute(bluetooth, new GetPIDUseCase.Callback() {
                @Override
                public void onGotPIDs(HashMap<String, String> pid) {
                    callback.onGotPID();
                    next();
                }

                @Override
                public void onError(RequestError error) {
                    callback.onPIDError();
                    finish();
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
}

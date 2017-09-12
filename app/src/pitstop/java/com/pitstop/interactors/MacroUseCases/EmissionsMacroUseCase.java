package com.pitstop.interactors.MacroUseCases;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.Interactor;
import com.pitstop.interactors.emissions.Post2141UseCase;
import com.pitstop.interactors.emissions.Post2141UseCaseImpl;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPIDUseCaseImpl;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Matt on 2017-08-28.
 */

public class EmissionsMacroUseCase {

    public interface Callback{
        void onStartPID();
        void onGotPID();
        void onErrorPID();
        void onStartPost2141();
        void onDonePost2141(JSONObject response);
        void onErrorPort2141();
        void onFinish();

    }

    private UseCaseComponent component;
    private BluetoothConnectionObservable bluetooth;
    private Callback callback;


    public Queue<Interactor> interactorQueue;

    private String pid2141;

    public EmissionsMacroUseCase(UseCaseComponent component, BluetoothConnectionObservable bluetooth, Callback callback){
        this.component = component;
        this.bluetooth = bluetooth;
        this.callback = callback;
        interactorQueue = new LinkedList<Interactor>();
        interactorQueue.add(component.getGetPIDUseCase());
        interactorQueue.add(component.getPost2141UseCase());
    }
    public void start(){
        next();
    }
    private void next(){
        if(interactorQueue.isEmpty()){finish();}
        Interactor current = interactorQueue.peek();
        interactorQueue.remove(current);

        if(current instanceof GetPIDUseCaseImpl){
          callback.onStartPID();
            ((GetPIDUseCaseImpl) current).execute(bluetooth, new GetPIDUseCase.Callback() {
                @Override
                public void onGotPIDs(HashMap<String, String> pid) {
                    if(pid.containsKey("2141")){
                        pid2141 = pid.get("2141");
                        callback.onGotPID();
                        next();
                    }else{
                        callback.onErrorPID();
                        finish();
                    }
                }

                @Override
                public void onError(RequestError error) {
                    System.out.println("Testing PID error");
                    callback.onErrorPID();
                    finish();
                }
            });
        }
        else if(current instanceof Post2141UseCaseImpl){
            if(bluetooth.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
                System.out.print("Testing scanner "+ bluetooth.getReadyDevice().getScannerId());
                ((Post2141UseCaseImpl) current).execute(pid2141, bluetooth.getReadyDevice().getScannerId(), new Post2141UseCase.Callback() {
                    @Override
                    public void onPIDPosted(JSONObject response) {
                        System.out.println("Testing json response"+response);
                        callback.onDonePost2141(response);
                        next();
                    }

                    @Override
                    public void onError(RequestError error) {
                        finish();
                    }
                });
            }else{
                System.out.println("Testing device not connected "+bluetooth.getDeviceState());
                finish();
            }
        }

        else{
            finish();
        }
    }
    private void finish(){
        callback.onFinish();
    }
}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothPidObserver;

import java.util.HashMap;

/**
 * Created by Matt on 2017-08-23.
 */

public class GetPIDUseCaseImpl implements GetPIDUseCase {

    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private BluetoothConnectionObservable bluetooth;

    public GetPIDUseCaseImpl(Handler useCaseHandler, Handler mainHandler){
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(BluetoothConnectionObservable bluetooth, Callback callback) {
        this.callback = callback;
        this.bluetooth = bluetooth;
        useCaseHandler.post(this);
    }

    private void onGotPIDs(HashMap<String, String> allPid, BluetoothPidObserver pidObserver){
        mainHandler.post(() -> {
            bluetooth.unsubscribe(pidObserver);
            callback.onGotPIDs(allPid);
        });
    }

    private void subscribeAndRequest(BluetoothPidObserver pidObserver){
        mainHandler.post(() -> {
            bluetooth.subscribe(pidObserver);
            bluetooth.requestAllPid();
        });
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        BluetoothPidObserver pidObserver = new BluetoothPidObserver() {
            @Override
            public void onGotAllPid(HashMap<String, String> allPid) {
                GetPIDUseCaseImpl.this.onGotPIDs(allPid,this);
            }

            @Override
            public void onErrorGettingAllPid() {
                GetPIDUseCaseImpl.this.onError(RequestError.getUnknownError());
            }
        };
        GetPIDUseCaseImpl.this.subscribeAndRequest(pidObserver);
    }
}

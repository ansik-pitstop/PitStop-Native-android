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
    private Handler handler;
    private BluetoothConnectionObservable bluetooth;

    public GetPIDUseCaseImpl(Handler handler){
        this.handler = handler;
    }

    @Override
    public void execute(BluetoothConnectionObservable bluetooth, Callback callback) {
        this.callback = callback;
        this.bluetooth = bluetooth;
        handler.post(this);
    }

    @Override
    public void run() {
        BluetoothPidObserver pidObserver = new BluetoothPidObserver() {
            @Override
            public void onGotAllPid(HashMap<String, String> allPid) {
                callback.onGotPIDs(allPid);
                bluetooth.unsubscribe(this);
            }

            @Override
            public void onErrorGettingAllPid() {
                callback.onError(RequestError.getUnknownError());
            }
        };
        bluetooth.subscribe(pidObserver);
        bluetooth.requestAllPid();
    }
}

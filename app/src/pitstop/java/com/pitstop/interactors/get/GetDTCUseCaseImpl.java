package com.pitstop.interactors.get;


import android.os.Handler;

import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothDtcObserver;

import java.util.HashMap;

/**
 * Created by Matt on 2017-08-23.
 */

public class GetDTCUseCaseImpl implements GetDTCUseCase {

    private Callback callback;
    private Handler handler;
    private BluetoothConnectionObservable bluetooth;

    public GetDTCUseCaseImpl(Handler handler){
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
        BluetoothDtcObserver dtcObserver = new BluetoothDtcObserver() {
            @Override
            public void onGotDtc(HashMap<String, Boolean> dtc) {
                callback.onGotDTCs(dtc);
                bluetooth.unsubscribe(this);
            }

            @Override
            public void onErrorGettingDtc() {
                callback.onError(RequestError.getUnknownError());
            }
        };
        bluetooth.subscribe(dtcObserver);
        bluetooth.requestDtcData();

    }
}

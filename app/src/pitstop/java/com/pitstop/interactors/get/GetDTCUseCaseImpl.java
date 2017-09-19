package com.pitstop.interactors.get;


import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothDtcObserver;

/**
 * Created by Matt on 2017-08-23.
 */

public class GetDTCUseCaseImpl implements GetDTCUseCase {

    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private BluetoothConnectionObservable bluetooth;

    public GetDTCUseCaseImpl(Handler useCaseHandler, Handler mainHandler){
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(BluetoothConnectionObservable bluetooth, Callback callback) {
        this.callback = callback;
        this.bluetooth = bluetooth;
        useCaseHandler.post(this);
    }

    private void subscribeAndRequest(BluetoothDtcObserver bluetoothDtcObserver){
        mainHandler.post(() -> {
            bluetooth.subscribe(bluetoothDtcObserver);
            bluetooth.requestDtcData();
        });
    }

    private void onGotDTCs(DtcPackage dtc, BluetoothDtcObserver bluetoothDtcObserver){
        mainHandler.post(() -> {
            callback.onGotDTCs(dtc);
            bluetooth.unsubscribe(bluetoothDtcObserver);
        });
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        BluetoothDtcObserver dtcObserver = new BluetoothDtcObserver() {
            @Override
            public void onGotDtc(DtcPackage dtc) {
                GetDTCUseCaseImpl.this.onGotDTCs(dtc,this);
            }

            @Override
            public void onErrorGettingDtc() {
                GetDTCUseCaseImpl.this.onError(RequestError.getUnknownError());
            }
        };
        subscribeAndRequest(dtcObserver);

    }
}

package com.pitstop.interactors.add;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.utils.Logger;


/**
 * Created by ishan on 2017-12-13.
 */

public class AddScannerUseCaseImpl implements AddScannerUseCase {
    private static final String TAG = AddScannerUseCaseImpl.class.getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;
    private String scannerId;
    private int carId;
    private ScannerRepository scannerRepository;
    private boolean carHasScanner;
    private String oldScannerId;

    public AddScannerUseCaseImpl(Handler useCaseHandler, Handler mainHandler, ScannerRepository scannerRepository) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.scannerRepository = scannerRepository;
    }

    @Override
    public void execute(boolean carHasScanner, String oldScannerID, int carId, String newScannerID, Callback callback) {
        this.carId = carId;
        this.scannerId = newScannerID;
        this.callback = callback;
        this.carHasScanner = carHasScanner;
        this.oldScannerId = oldScannerID;
        this.useCaseHandler.post(this);

    }

    @Override
    public void run() {
        ObdScanner obdScanner = new ObdScanner(carId, scannerId); //Scanner to be added
        obdScanner.setStatus(true);
        Log.d(TAG, "car doesnt have scanner");
        addScanner(obdScanner, new Callback() {
            @Override
            public void onDeviceAlreadyActive() {
                //Another user has this scanner
                AddScannerUseCaseImpl.this.onDeviceAlreadyActive();
            }

            @Override
            public void onScannerCreated() {
                //Scanner created
                Log.d(TAG, "Created new scanner, onSuccess()");
                AddScannerUseCaseImpl.this.onSuccess();
            }

            @Override
            public void onError(RequestError error) {
                AddScannerUseCaseImpl.this.onError(error);
            }
        });
    }

    private void onDeviceAlreadyActive() {
        Logger.getInstance().logI(TAG, "Use case finished: device already active"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> this.callback.onDeviceAlreadyActive());
    }

    private void onSuccess() {
        Logger.getInstance().logI(TAG, "Use case finished: success"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> this.callback.onScannerCreated());
    }

    private void onError(RequestError error) {
        Logger.getInstance().logE(TAG, "Use case returned error: err=" + error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> this.callback.onError(error));

    }


    private void addScanner(ObdScanner obdScanner, Callback callback) {
        scannerRepository.createScanner(obdScanner, new Repository.Callback() {
            @Override
            public void onSuccess(Object data) {
                callback.onScannerCreated();
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}

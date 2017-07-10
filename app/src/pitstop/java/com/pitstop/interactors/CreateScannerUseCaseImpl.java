package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.ObdScanner;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class CreateScannerUseCaseImpl implements CreateScannerUseCase {

    private ScannerRepository scannerRepository;
    private Handler handler;
    private Callback callback;
    private ObdScanner obdScanner;

    public CreateScannerUseCaseImpl(ScannerRepository scannerRepository, Handler handler){
        this.scannerRepository = scannerRepository;
        this.handler = handler;
    }

    @Override
    public void execute(ObdScanner obdScanner, Callback callback) {
        this.obdScanner = obdScanner;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        //Check to see if this scanner is already active
        scannerRepository.getScanner(obdScanner.getScannerId(), new Repository.Callback<ObdScanner>() {

            @Override
            public void onSuccess(ObdScanner data) {

                //is already active
                if (data.getStatus()){
                    callback.onDeviceAlreadyActive();
                    return;
                }

                //Create scanner otherwise
                scannerRepository.createScanner(obdScanner, new Repository.Callback() {

                    @Override
                    public void onSuccess(Object data) {
                       callback.onScannerCreated();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError();
                    }
                });

            }

            @Override
            public void onError(String error) {
                callback.onError();
            }
        });
    }


}

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
        scannerRepository.getScanner(obdScanner.getScannerId(), true, new Repository.Callback<ObdScanner>() {
            @Override
            public void onSuccess(ObdScanner data) {

            }

            @Override
            public void onError(int error) {

            }
        });
        //Create scanner both locally and remotely
    }


}

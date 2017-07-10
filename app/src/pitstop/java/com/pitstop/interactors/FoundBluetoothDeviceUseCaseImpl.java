package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.repositories.ScannerRepository;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class FoundBluetoothDeviceUseCaseImpl implements FoundBluetoothDeviceUseCase {

    private ScannerRepository scannerRepository;
    private Handler handler;
    private Callback callback;
    private String scannerId;
    private String scannerName;

    public FoundBluetoothDeviceUseCaseImpl(ScannerRepository scannerRepository, Handler handler
            , Callback callback) {

        this.scannerRepository = scannerRepository;
        this.handler = handler;
        this.callback = callback;
    }

    @Override
    public void execute(String scannerId, String scannerName) {
        this.scannerId = scannerId;
        this.scannerName = scannerName;
        handler.post(this);
    }

    @Override
    public void run() {

        //Not  IDD device
        if (scannerName == null || !scannerName.contains(BT_DEVICE_NAME)){
            callback.onDeviceInvalid();
            return;
        }

        //215 device with broken name
        if (scannerName.startsWith(BT_DEVICE_NAME_215) && scannerName.endsWith(BT_DEVICE_NAME_BROKEN)){
            //Connect with device and check its VIN
            callback.onDevice215Broken();
        }
        //215 device with good name
        else if (scannerName.startsWith(BT_DEVICE_NAME_215)){
            callback.onDeviceValid();
        }
        //212 device
        else if (scannerName.startsWith(BT_DEVICE_NAME_212)){
            callback.onDeviceValid();
        }


    }
}

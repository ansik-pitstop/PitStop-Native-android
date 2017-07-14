package com.pitstop.interactors;

import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.models.ObdScanner;

/**
 * Validates scanner to see if its paired with another car and if so
 * creates the scanner both locally and remotely
 *
 * Created by Karol Zdebel on 7/10/2017.
 */

public interface CreateScannerUseCase extends Interactor {

    interface Callback{
        void onScannerCreated();
        void onSuccess();
        void onDeviceIdOverrideNeeded();
        void onDeviceInvalid();
        void onDeviceAlreadyActive();
        void onError();
    }

    void execute(ObdScanner obdScanner, Callback callback, ParameterPackage parameterPackage);
}

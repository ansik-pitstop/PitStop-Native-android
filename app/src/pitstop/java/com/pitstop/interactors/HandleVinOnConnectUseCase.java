package com.pitstop.interactors;

import com.pitstop.bluetooth.dataPackages.ParameterPackage;

/**
 * Validates scanner to see if its paired with another car and if so
 * creates the scanner both locally and remotely
 *
 * Only for 215, not tested on 212
 *
 * Created by Karol Zdebel on 7/10/2017.
 */

public interface HandleVinOnConnectUseCase extends Interactor {

    interface Callback{
        void onSuccess();
        void onDeviceIdOverrideNeeded();
        void onDeviceInvalid();
        void onDeviceAlreadyActive(); //Another user has this scanner
        void onError();
    }

    void execute(ParameterPackage parameterPackage, Callback callback);
}

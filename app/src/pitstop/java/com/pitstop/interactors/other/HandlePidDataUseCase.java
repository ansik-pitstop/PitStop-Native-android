package com.pitstop.interactors.other;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public interface HandlePidDataUseCase extends Interactor {
    interface Callback{
        void onSuccess();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(PidPackage pidPackage, Callback callback);
}

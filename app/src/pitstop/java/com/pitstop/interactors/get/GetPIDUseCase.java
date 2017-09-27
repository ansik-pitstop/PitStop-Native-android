package com.pitstop.interactors.get;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;

/**
 * Created by Matt on 2017-08-23.
 */

public interface GetPIDUseCase extends Interactor {
    interface Callback{
        void onGotPIDs(PidPackage pid);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(BluetoothConnectionObservable bluetooth, Callback callback);
}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;

import java.util.HashMap;

/**
 * Created by Matt on 2017-08-23.
 */

public interface GetPIDUseCase extends Interactor {
    interface Callback{
        void onGotPIDs(HashMap<String, String> pid);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(BluetoothConnectionObservable bluetooth, Callback callback);
}

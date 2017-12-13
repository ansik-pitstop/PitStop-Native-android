package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by ishan on 2017-12-13.
 */

public interface AddScannerUseCase extends Interactor{
    interface Callback{
         void onDeviceAlreadyActive();
         void onScannerCreated();
         void onError(RequestError error);
    }

    void execute(boolean carHasScanner,String oldScannerID,  int carId, String scannerID, Callback callback);

}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by ishan on 2017-09-27.
 */

public interface GetLicensePlateUseCase extends Interactor {

    interface Callback{
        void onLicensePlateGot(String licensePlate);
        void onError(RequestError error);
    }
    void execute(int CarID, Callback callback);
}

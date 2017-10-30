package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public interface GetCarByDeviceIdUseCase extends Interactor{
    interface Callback{
        void onGotCar(Car car);
        void onNoCarFound();
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(String deviceId ,Callback callback);
}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 9/25/2017.
 */

public interface GetCarByVinUseCase extends Interactor {
    interface Callback{
        void onGotCar(Car car);
        void onNoCarFound();
        void onError(RequestError error);
    }

    void execute(String vin, Callback callback);
}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-06-13.
 */

public interface GetCarsByUserIdUseCase extends Interactor {
    interface Callback{
        void onGotCar(Car car);
        void onNoCarFound();
        void onError(RequestError error);
    }

    //Execute the use case
    void execute(GetCarsByUserIdUseCase.Callback callback);
}

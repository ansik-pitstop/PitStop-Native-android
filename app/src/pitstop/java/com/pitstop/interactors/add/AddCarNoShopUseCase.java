package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-07-27.
 */

public interface AddCarNoShopUseCase extends Interactor {
    interface Callback{
        void onCarAddedWithBackendShop(Car car);
        void onCarAdded(Car car);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(Car pendingCar, String scannerName, String eventSource, Callback callback);
}

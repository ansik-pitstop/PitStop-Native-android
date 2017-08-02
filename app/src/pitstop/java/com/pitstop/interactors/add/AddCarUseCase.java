package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-07-27.
 */

public interface AddCarUseCase extends Interactor {
    interface Callback{
        void onCarAddedWithBackendShop(Car car);
        void onCarAdded(Car car);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(String vin, double baseMileage, int userId, String scannerId, int shopId
            ,String scannerName, String eventSource, boolean carHasShop, Callback callback);
}

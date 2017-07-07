package com.pitstop.interactors;

import com.pitstop.models.Car;

/**
 * Created by Matthew on 2017-06-20.
 */

public interface GetCarByCarIdUseCase extends Interactor {
    interface Callback{
        void onCarGot(Car car);
        void onError();
    }

    //Executes usecase
    void execute(int carId,Callback callback);
}

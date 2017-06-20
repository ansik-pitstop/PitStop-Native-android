package com.pitstop.interactors;

import com.pitstop.models.Car;

import java.util.List;

/**
 * Created by xirax on 2017-06-13.
 */

public interface GetCarsByUserIdUseCase extends Interactor {
    interface Callback{
        void onCarsRetrieved(List<Car> cars);
        void onError();
    }

    //Execute the use case
    void execute(GetCarsByUserIdUseCase.Callback callback);
}

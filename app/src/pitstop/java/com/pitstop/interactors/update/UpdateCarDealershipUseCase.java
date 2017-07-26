package com.pitstop.interactors.update;


import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-06-20.
 */

public interface UpdateCarDealershipUseCase extends Interactor {
    interface Callback{
        void onCarDealerUpdated();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(int carId, Dealership dealership,String eventSource, Callback callback);
}

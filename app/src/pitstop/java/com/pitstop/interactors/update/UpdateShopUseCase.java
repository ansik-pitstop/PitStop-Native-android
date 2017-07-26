package com.pitstop.interactors.update;


import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-06-27.
 */

public interface UpdateShopUseCase extends Interactor {
    interface Callback{
        void onShopUpdated();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(Dealership dealership,String eventSource, Callback callback);
}

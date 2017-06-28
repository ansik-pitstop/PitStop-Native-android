package com.pitstop.interactors;


import com.pitstop.models.Dealership;

/**
 * Created by Matthew on 2017-06-27.
 */

public interface UpdateShopUseCase extends Interactor {
    interface Callback{
        void onShopUpdated();
        void onError();
    }

    //Executes the use case
    void execute(Dealership dealership, Callback callback);
}

package com.pitstop.interactors;

import com.pitstop.models.Dealership;

/**
 * Created by Matthew on 2017-06-21.
 */

public interface AddShopUseCase extends Interactor {
    interface Callback{
        void onShopAdded();
        void onError();
    }

    //Executes usecase
    void execute(Dealership dealership, Callback callback);
}

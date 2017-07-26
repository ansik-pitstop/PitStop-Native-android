package com.pitstop.interactors.remove;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-06-26.
 */

public interface RemoveShopUseCase extends Interactor {
    interface Callback{
        void onShopRemoved();
        void onCantRemoveShop();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(Dealership dealership, Callback callback);
}

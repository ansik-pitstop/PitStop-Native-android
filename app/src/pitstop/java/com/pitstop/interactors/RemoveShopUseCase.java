package com.pitstop.interactors;

import com.pitstop.models.Dealership;

/**
 * Created by Matthew on 2017-06-26.
 */

public interface RemoveShopUseCase extends Interactor {
    interface Callback{
        void onShopRemoved();
        void onCantRemoveShop();
        void onError();
    }

    //Executes the use case
    void execute(Dealership dealership, Callback callback);
}

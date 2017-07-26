package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-06-21.
 */

public interface AddShopUseCase extends Interactor {
    interface Callback{
        void onShopAdded();
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(Dealership dealership, Callback callback);
}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by Matthew on 2017-06-26.
 */

public interface GetUserShopsUseCase extends Interactor {
    interface Callback{
        void onShopGot(List<Dealership> dealerships);
        void onError(RequestError error);
    }

    //Execute the use case
    void execute(Callback callback);
}

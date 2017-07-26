package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-06-30.
 */

public interface GetPlaceDetailsUseCase extends Interactor {
    interface Callback{
        void onDetailsGot(Dealership dealership);
        void onError(RequestError error);
    }

    //Execute the use case
    void execute(Dealership dealership, Callback callback);
}

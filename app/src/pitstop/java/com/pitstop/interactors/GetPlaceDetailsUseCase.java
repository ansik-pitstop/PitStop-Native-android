package com.pitstop.interactors;

import com.pitstop.models.Dealership;

/**
 * Created by Matthew on 2017-06-30.
 */

public interface GetPlaceDetailsUseCase extends Interactor{
    interface Callback{
        void onDetailsGot(Dealership dealership);
        void onError();
    }

    //Execute the use case
    void execute(Dealership dealership, Callback callback);
}

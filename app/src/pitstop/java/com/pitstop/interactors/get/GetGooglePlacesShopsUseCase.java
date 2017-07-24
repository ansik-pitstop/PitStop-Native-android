package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Dealership;

import java.util.List;


/**
 * Created by Matthew on 2017-06-29.
 */

public interface GetGooglePlacesShopsUseCase extends Interactor {
    interface CallbackShops{
        void onShopsGot(List<Dealership> dealerships);
        void onError();
    }

    //Executes usecase
    void execute(double latitude, double longitude, String query, CallbackShops callback);
}

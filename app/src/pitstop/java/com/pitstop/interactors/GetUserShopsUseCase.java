package com.pitstop.interactors;

import com.pitstop.models.Dealership;

import java.util.List;

/**
 * Created by Matthew on 2017-06-26.
 */

public interface GetUserShopsUseCase extends Interactor {
    interface Callback{
        void onShopGot(List<Dealership> dealerships);
        void onError();
    }

    //Execute the use case
    void execute(Callback callback);
}

package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by ishan on 2017-09-28.
 */

public interface GetCarStyleIDUseCase extends Interactor {

    interface Callback{
        void onStyleIDGot(String styleID);
        void onError(RequestError error);
    }

    void execute(String VIN, GetCarStyleIDUseCase.Callback callback);
}

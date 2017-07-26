package com.pitstop.interactors.update;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-06-15.
 */

public interface UpdateUserPhoneUseCase extends Interactor {
    interface Callback{
        void onUserPhoneUpdated();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(String phone, Callback callback);
}

package com.pitstop.interactors.get;


import com.pitstop.interactors.Interactor;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;


/**
 * Created by Matt on 2017-06-14.
 */

public interface GetCurrentUserUseCase extends Interactor {
    interface Callback{
        void onUserRetrieved(User user);
        void onError(RequestError error);
    }

    //Execute the use case
    void execute(GetCurrentUserUseCase.Callback callback);
}

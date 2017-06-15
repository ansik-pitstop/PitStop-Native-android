package com.pitstop.interactors;


import com.pitstop.models.User;


/**
 * Created by xirax on 2017-06-14.
 */

public interface GetCurrentUserUseCase extends Interactor {
    interface Callback{
        void onUserRetrieved(User user);
        void onError();
    }

    //Execute the use case
    void execute(GetCurrentUserUseCase.Callback callback);
}

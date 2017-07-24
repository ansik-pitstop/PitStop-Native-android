package com.pitstop.interactors.update;

import com.pitstop.interactors.Interactor;

/**
 * Created by Matt on 2017-06-15.
 */

public interface UpdateUserNameUseCase extends Interactor {
    interface Callback{
        void onUserNameUpdated();
        void onError();
    }

    //Executes the use case
    void execute(String name, Callback callback);
}

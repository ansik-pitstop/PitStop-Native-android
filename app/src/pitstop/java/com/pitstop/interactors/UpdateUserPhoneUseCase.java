package com.pitstop.interactors;

/**
 * Created by xirax on 2017-06-15.
 */

public interface UpdateUserPhoneUseCase extends Interactor {
    interface Callback{
        void onUserPhoneUpdated();
        void onError();
    }

    //Executes the use case
    void execute(String phone, Callback callback);
}

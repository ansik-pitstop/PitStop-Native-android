package com.pitstop.interactors.other;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Matthew on 2017-07-17.
 */

public interface RequestServiceUseCase extends Interactor {
    interface Callback{
    void onServicesRequested();
    void onError(RequestError error);
}

    //Executes the use case
    void execute(String state,String timeStamp, String comments,Callback callback);
}

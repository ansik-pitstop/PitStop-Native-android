package com.pitstop.interactors.other;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

import java.util.Date;

/**
 * Created by Matthew on 2017-07-17.
 */

public interface RequestServiceUseCase extends Interactor {
    interface Callback{
    void onServicesRequested();
    void onError(RequestError error);
}

    //Executes the use case
    void execute(String state, Date date, String comments, Callback callback);
}

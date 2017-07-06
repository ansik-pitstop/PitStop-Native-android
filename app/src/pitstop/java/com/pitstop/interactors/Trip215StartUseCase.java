package com.pitstop.interactors;

import com.pitstop.models.Trip215;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Trip215StartUseCase extends Interactor{
    interface Callback{
        void onTripStartSuccess();
        void onError();
    }

    void execute(Trip215 tripStart, Callback callback);
}

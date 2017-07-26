package com.pitstop.interactors.set;

import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface SetFirstCarAddedUseCase extends Runnable {
    interface Callback{
        void onFirstCarAddedSet();
        void onError(RequestError error);
    }

    void execute(boolean sent, Callback callback);
}

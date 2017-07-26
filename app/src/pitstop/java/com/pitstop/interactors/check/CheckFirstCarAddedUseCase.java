package com.pitstop.interactors.check;

import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface CheckFirstCarAddedUseCase extends Runnable{

    interface Callback{
        void onFirstCarAddedChecked(boolean added);
        void onError(RequestError error);
    }

    void execute(Callback callback);
}

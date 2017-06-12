package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface SetFirstCarAddedUseCase extends Runnable {
    interface Callback{
        void onFirstCarAddedSet();
        void onError();
    }

    void execute(boolean sent, Callback callback);
}

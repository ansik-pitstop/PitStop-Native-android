package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface CheckGreetingsSentUseCase extends Runnable{

    interface Callback{
        void onGotWhetherSmoochSent(boolean sent);
        void onError();
    }

    void execute(Callback callback);
}

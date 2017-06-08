package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface SetSmoochMessageSentUseCase extends Runnable {
    interface Callback{
        void onUserSmoochMessageVarSet();
        void onError();
    }

    void execute(boolean sent, int userId, Callback callback);
}

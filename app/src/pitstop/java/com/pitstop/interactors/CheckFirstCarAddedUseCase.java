package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public interface CheckFirstCarAddedUseCase extends Runnable{

    interface Callback{
        void onFirstCarAddedChecked(boolean added);
        void onError();
    }

    void execute(Callback callback);
}

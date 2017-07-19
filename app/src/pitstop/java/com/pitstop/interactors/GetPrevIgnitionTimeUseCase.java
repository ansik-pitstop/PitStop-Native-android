package com.pitstop.interactors;

/**
 * Get the previous trip's ignition time
 *
 * Created by Karol Zdebel on 7/19/2017.
 */

public interface GetPrevIgnitionTimeUseCase extends Interactor {

    interface Callback{
        void onGotIgnitionTime(long ignitionTime);
        void onNoneExists();
        void onError(String error);
    }

    void execute(String scannerName, Callback callback);
}

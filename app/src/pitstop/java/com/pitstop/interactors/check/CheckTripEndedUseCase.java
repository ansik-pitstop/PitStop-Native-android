package com.pitstop.interactors.check;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 8/25/2017.
 */

public interface CheckTripEndedUseCase extends Interactor {
    interface Callback{
        void onGotLatestTripStatus(boolean ended, long rtcTime);
        void onNoLatestTripExists();
        void onError(RequestError error);
    }

    void execute(String deviceId, Callback callback);
}

package com.pitstop.interactors.other;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Trip215StartUseCase extends Interactor {
    interface Callback{
        void onRealTimeTripStartSuccess();
        void onHistoricalTripStartSuccess();
        void onError(RequestError error);
    }

    void execute(TripInfoPackage tripInfoPackage, long terminalRTCTime, Callback callback);
}

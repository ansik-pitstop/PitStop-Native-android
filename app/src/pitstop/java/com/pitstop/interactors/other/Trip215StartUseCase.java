package com.pitstop.interactors.other;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.interactors.Interactor;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Trip215StartUseCase extends Interactor {
    interface Callback{
        void onRealTimeTripStartSuccess();
        void onHistoricalTripStartSuccess();
        void onError();
    }

    void execute(TripInfoPackage tripInfoPackage, long terminalRTCTime, Callback callback);
}

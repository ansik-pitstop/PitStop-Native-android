package com.pitstop.interactors;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Trip215EndUseCase extends Interactor{

    interface Callback{
        void onHistoricalTripEndSuccess();
        void onRealTimeTripEndSuccess();
        void onStartTripNotFound();
        void onError();
    }

    void execute(TripInfoPackage tripInfoPackage, long terminalRTCTime, Callback callback);
}

package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Trip215;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215StartUseCaseImpl implements Trip215StartUseCase {

    private Device215TripRepository device215TripRepository;
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private long terminalRTCTime;
    private Callback callback;


    public Trip215StartUseCaseImpl(Device215TripRepository device215TripRepository, Handler handler){

        this.device215TripRepository = device215TripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(TripInfoPackage tripInfoPackage,long terminalRTCTime, Callback callback) {
        this.callback = callback;
        this.terminalRTCTime = terminalRTCTime;
        this.tripInfoPackage = tripInfoPackage;
        handler.post(this);
    }

    @Override
    public void run() {
        Trip215 tripStart = convertToTrip215(tripInfoPackage);
        device215TripRepository.storeTripStart(tripStart, new Repository.Callback<Trip215>() {
            @Override
            public void onSuccess(Trip215 data) {
                if (tripStart.getRtcTime() > terminalRTCTime){
                    callback.onRealTimeTripStartSuccess();
                }
                else{
                    callback.onHistoricalTripStartSuccess();
                }
            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });
    }

    private Trip215 convertToTrip215(TripInfoPackage tripInfoPackage){
        return new Trip215(tripInfoPackage.tripId,tripInfoPackage.mileage
                ,tripInfoPackage.rtcTime,tripInfoPackage.deviceId);
    }
}

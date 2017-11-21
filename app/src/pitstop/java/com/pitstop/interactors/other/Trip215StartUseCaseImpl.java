package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215StartUseCaseImpl implements Trip215StartUseCase {

    private final String TAG = getClass().getSimpleName();
    private final int HISTORICAL_OFFSET = 100; //Terminal rtc time takes some time to retrieve

    private Device215TripRepository device215TripRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private TripInfoPackage tripInfoPackage;
    private Callback callback;

    public Trip215StartUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler useCaseHandler, Handler mainHandler){

        this.device215TripRepository = device215TripRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(TripInfoPackage tripInfoPackage, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: tripInfoPackage="+tripInfoPackage
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.tripInfoPackage = tripInfoPackage;
        useCaseHandler.post(this);
    }

    private void onRealTimeTripStartSuccess(){
        Logger.getInstance().logI(TAG,"Use case execution finished: real time trip start success"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onRealTimeTripStartSuccess());
    }

    private void onHistoricalTripStartSuccess(){
        Logger.getInstance().logI(TAG,"Use case execution finished: historical trip start success"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onHistoricalTripStartSuccess());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        Trip215 tripStart = convertToTrip215(tripInfoPackage);
        device215TripRepository.storeTripStart(tripStart, new Repository.Callback<Trip215>() {
            @Override
            public void onSuccess(Trip215 data) {
                if (tripStart.getRtcTime() > tripInfoPackage.terminalRtcTime - HISTORICAL_OFFSET){
                    Trip215StartUseCaseImpl.this.onRealTimeTripStartSuccess();
                }
                else{
                    Trip215StartUseCaseImpl.this.onHistoricalTripStartSuccess();
                }
            }

            @Override
            public void onError(RequestError error) {

                Log.d(TAG,"onError! error:"+error.getMessage());
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    Log.d(TAG,"Storing trip start locally due to error!");
                    device215TripRepository.storeTripLocally(tripInfoPackage);
                }
                Trip215StartUseCaseImpl.this.onError(error);
            }
        });
    }

    private Trip215 convertToTrip215(TripInfoPackage tripInfoPackage){
        return new Trip215(Trip215.TRIP_START,tripInfoPackage.tripId,tripInfoPackage.mileage
                ,tripInfoPackage.rtcTime,tripInfoPackage.deviceId);
    }
}

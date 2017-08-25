package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.RetrievedTrip215;
import com.pitstop.models.Trip215Start;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215StartUseCaseImpl implements Trip215StartUseCase {

    private final String TAG = getClass().getSimpleName();
    private final int HISTORICAL_OFFSET = 100; //Terminal rtc time takes some time to retrieve

    private Device215TripRepository device215TripRepository;
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private Callback callback;

    public Trip215StartUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler handler){

        this.device215TripRepository = device215TripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(TripInfoPackage tripInfoPackage, Callback callback) {
        this.callback = callback;
        this.tripInfoPackage = tripInfoPackage;
        handler.post(this);
    }

    @Override
    public void run() {
        Trip215Start tripStart = convertToTrip215(tripInfoPackage);
        device215TripRepository.storeTripStart(tripStart, new Repository.Callback<RetrievedTrip215>() {
            @Override
            public void onSuccess(RetrievedTrip215 data) {
                if (tripStart.getRtcTime() > tripInfoPackage.terminalRtcTime - HISTORICAL_OFFSET){
                    callback.onRealTimeTripStartSuccess();
                }
                else{
                    callback.onHistoricalTripStartSuccess();
                }
            }

            @Override
            public void onError(RequestError error) {

                Log.d(TAG,"onError! error:"+error.getMessage());
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    Log.d(TAG,"Storing trip start locally due to error!");
                    device215TripRepository.storeTripLocally(tripInfoPackage);
                }
                callback.onError(error);
            }
        });
    }

    private Trip215Start convertToTrip215(TripInfoPackage tripInfoPackage){
        return new Trip215Start(tripInfoPackage.tripId,tripInfoPackage.deviceId, tripInfoPackage.mileage
                ,tripInfoPackage.rtcTime);
    }
}

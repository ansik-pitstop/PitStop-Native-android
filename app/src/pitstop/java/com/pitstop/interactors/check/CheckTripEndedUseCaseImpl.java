package com.pitstop.interactors.check;

import android.os.Handler;

import com.pitstop.models.RetrievedTrip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 8/25/2017.
 */

public class CheckTripEndedUseCaseImpl implements CheckTripEndedUseCase {

    private Device215TripRepository tripRepository;
    private Callback callback;
    private Handler handler;
    private String deviceId;

    public CheckTripEndedUseCaseImpl(Device215TripRepository tripRepository, Handler handler) {
        this.tripRepository = tripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String deviceId, Callback callback) {
        if (deviceId == null) callback.onError(RequestError.getUnknownError());
        this.deviceId = deviceId;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        tripRepository.retrieveLatestTrip(deviceId, new Repository.Callback<RetrievedTrip215>() {
            @Override
            public void onSuccess(RetrievedTrip215 data) {
                if (data != null){
                    callback.onGotLatestTripStatus(data.isEnded(), data.getRtcTime());
                }
                //No trip exists at all, therefore trip is not ended
                else{
                    callback.onNoLatestTripExists();
                }
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}

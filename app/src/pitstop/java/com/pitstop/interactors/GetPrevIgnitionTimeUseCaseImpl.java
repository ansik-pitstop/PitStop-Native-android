package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Trip215;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/19/2017.
 */

public class GetPrevIgnitionTimeUseCaseImpl implements GetPrevIgnitionTimeUseCase {

    private Device215TripRepository device215TripRepository;
    private String scannerName;
    private Callback callback;
    private Handler handler;

    public GetPrevIgnitionTimeUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler handler){
        this.device215TripRepository = device215TripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String scannerName, Callback callback) {
        this.scannerName = scannerName;
        this.callback = callback;

        handler.post(this);
    }

    @Override
    public void run() {

        device215TripRepository.retrieveLatestTrip(scannerName, new Repository.Callback<Trip215>() {

            @Override
            public void onSuccess(Trip215 data) {

                if (data == null){
                    callback.onNoneExists();
                    return;
                }

                //Trip id is the ignition time, this probably isn't the best way of doing this
                callback.onGotIgnitionTime(data.getTripId());
            }

            @Override
            public void onError(int error) {
                callback.onError(null);
            }
        });
    }
}

package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Trip215;
import com.pitstop.repositories.Device215TripRepository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215StartUseCaseImpl implements Trip215StartUseCase {

    private Device215TripRepository device215TripRepository;
    private Handler handler;
    private Trip215 tripStart;
    private Callback callback;


    public Trip215StartUseCaseImpl(Device215TripRepository device215TripRepository, Handler handler){
        this.device215TripRepository = device215TripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Trip215 tripStart, Callback callback) {
        this.callback = callback;
        this.tripStart = tripStart;

        handler.post(this);
    }

    @Override
    public void run() {

    }
}

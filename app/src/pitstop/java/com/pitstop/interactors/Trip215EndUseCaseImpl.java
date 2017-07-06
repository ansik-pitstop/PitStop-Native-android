package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Trip215;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215EndUseCaseImpl implements Trip215EndUseCase {

    private Device215TripRepository device215TripRepository;
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private Callback callback;

    public Trip215EndUseCaseImpl(Device215TripRepository device215TripRepository, Handler handler) {
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

        //Get latest trip
        device215TripRepository.retrieveLatestTrip(tripInfoPackage.deviceId, new Repository.Callback<Trip215>() {
            @Override
            public void onSuccess(Trip215 data) {

                Trip215 tripEnd = convertToTrip215End(tripInfoPackage,data);

                //Store trip end
                device215TripRepository.storeTripEnd(tripEnd, new Repository.Callback() {
                    @Override
                    public void onSuccess(Object data) {
                        callback.onTripEndSuccess();
                    }

                    @Override
                    public void onError(int error) {
                        callback.onError();
                    }
                });

            }

            @Override
            public void onError(int error) {

            }
        });
    }

    private Trip215 convertToTrip215End(TripInfoPackage tripInfoPackage, Trip215 tripStart){
        double tripMileage = tripInfoPackage.mileage - tripStart.getMileage();

        return new Trip215(tripInfoPackage.tripId,tripMileage,tripInfoPackage.rtcTime
                ,tripInfoPackage.deviceId);
    }
}

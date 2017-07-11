package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Car;
import com.pitstop.models.Trip215;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215StartUseCaseImpl implements Trip215StartUseCase {

    private Device215TripRepository device215TripRepository;
    private UserRepository userRepository;
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private long terminalRTCTime;
    private Callback callback;


    public Trip215StartUseCaseImpl(Device215TripRepository device215TripRepository
            , UserRepository userRepository, Handler handler){

        this.device215TripRepository = device215TripRepository;
        this.userRepository = userRepository;
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

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                Trip215 tripStart = convertToTrip215(tripInfoPackage, car.getTotalMileage());

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

            @Override
            public void onNoCarSet() {
                callback.onError();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });

    }

    private Trip215 convertToTrip215(TripInfoPackage tripInfoPackage, double totalMileage){
        return new Trip215(tripInfoPackage.tripId, totalMileage
                ,tripInfoPackage.rtcTime,tripInfoPackage.deviceId);
    }
}

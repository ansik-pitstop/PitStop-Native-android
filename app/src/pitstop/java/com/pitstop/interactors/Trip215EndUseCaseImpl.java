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

public class Trip215EndUseCaseImpl implements Trip215EndUseCase {

    private Device215TripRepository device215TripRepository;
    private UserRepository userRepository;
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private long terminalRTCTime;       //RTC time recorded upon connecting to device
    private Callback callback;

    public Trip215EndUseCaseImpl(Device215TripRepository device215TripRepository
            , UserRepository userRepository, Handler handler) {
        this.device215TripRepository = device215TripRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(TripInfoPackage tripInfoPackage, long terminalRTCTime, Callback callback) {
        this.callback = callback;
        this.tripInfoPackage = tripInfoPackage;
        this.terminalRTCTime = terminalRTCTime;

        handler.post(this);
    }

    @Override
    public void run() {

        //Get car so that total mileage can be calculated
        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                //Get latest trip
                device215TripRepository.retrieveLatestTrip(tripInfoPackage.deviceId, new Repository.Callback<Trip215>() {
                    @Override
                    public void onSuccess(Trip215 data) {

                        Trip215 tripEnd = convertToTrip215End(tripInfoPackage, data
                                , car.getTotalMileage());

                        //Store trip end
                        device215TripRepository.storeTripEnd(tripEnd, new Repository.Callback() {
                            @Override
                            public void onSuccess(Object data) {

                                //Send notification if a real time update occurred
                                if (tripInfoPackage.rtcTime > terminalRTCTime){
                                    callback.onRealTimeTripEndSuccess();

                                }
                                else{
                                    callback.onRealTimeTripEndSuccess();
                                }
                            }

                            @Override
                            public void onError(int error) {
                                callback.onError();
                            }
                        });

                    }

                    @Override
                    public void onError(int error) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError() {

            }
        });
    }

    private Trip215 convertToTrip215End(TripInfoPackage tripInfoPackage, Trip215 tripStart
            , double totalStartMileage){
        double tripMileage = tripInfoPackage.mileage - tripStart.getMileage()
                + totalStartMileage;

        return new Trip215(tripInfoPackage.tripId,tripMileage,tripInfoPackage.rtcTime
                ,tripInfoPackage.deviceId);
    }
}

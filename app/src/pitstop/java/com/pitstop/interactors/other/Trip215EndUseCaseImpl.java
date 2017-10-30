package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Trip215EndUseCaseImpl implements Trip215EndUseCase {

    private final String TAG = getClass().getSimpleName();
    private final int HISTORICAL_OFFSET = 100; //Terminal rtc time takes some time to retrieve

    private Device215TripRepository device215TripRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private TripInfoPackage tripInfoPackage;
    private Callback callback;

    public Trip215EndUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.device215TripRepository = device215TripRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(TripInfoPackage tripInfoPackage, Callback callback) {
        this.callback = callback;
        this.tripInfoPackage = tripInfoPackage;
        useCaseHandler.post(this);
    }

    private void onHistoricalTripEndSuccess(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onHistoricalTripEndSuccess();
            }
        });
    }
    private void onRealTimeTripEndSuccess(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onRealTimeTripEndSuccess();
            }
        });
    }
    private void onStartTripNotFound(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStartTripNotFound();
            }
        });
    }
    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    @Override
    public void run() {

        //Get latest trip
        device215TripRepository.retrieveLatestTrip(tripInfoPackage.deviceId, new Repository.Callback<Trip215>() {
            @Override
            public void onSuccess(Trip215 data) {

                //Latest trip does not exist, don't store trip end
                if (data == null || data.getTripIdRaw() != tripInfoPackage.tripId){
                    Trip215EndUseCaseImpl.this.onStartTripNotFound();
                    return;
                }

                //Another trip start got posted before onSuccess called, ignore trip end
                //Back-end should have the logic implemented to take care of this case
                if (data.getMileage() > tripInfoPackage.mileage){
                    Trip215EndUseCaseImpl.this.onRealTimeTripEndSuccess();
                    return;
                }

                Trip215 tripEnd = convertToTrip215End(tripInfoPackage,data);

                //Store trip end
                device215TripRepository.storeTripEnd(tripEnd, new Repository.Callback() {
                    @Override
                    public void onSuccess(Object data) {

                        //Send notification if a real time updateMileage occurred
                        if (tripInfoPackage.rtcTime > tripInfoPackage.terminalRtcTime
                                - HISTORICAL_OFFSET){
                            Trip215EndUseCaseImpl.this.onRealTimeTripEndSuccess();

                        }
                        else{
                            Trip215EndUseCaseImpl.this.onHistoricalTripEndSuccess();
                        }
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"onError() error: "+error.getMessage());
                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            Log.d(TAG,"Storing trip locally due to error.");
                            device215TripRepository.storeTripLocally(tripInfoPackage);
                        }
                        Trip215EndUseCaseImpl.this.onError(error);
                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"onError() error: "+error.getMessage());
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    Log.d(TAG,"Storing trip locally due to error.");
                    device215TripRepository.storeTripLocally(tripInfoPackage);
                }
                Trip215EndUseCaseImpl.this.onError(error);
            }
        });
    }

    //If latest trip succeeded
    private Trip215 convertToTrip215End(TripInfoPackage tripInfoPackage, Trip215 tripStart){
        double tripMileage = tripInfoPackage.mileage - tripStart.getMileage();

        return new Trip215(Trip215.TRIP_END,tripStart.getTripId(), tripInfoPackage.tripId,tripMileage,tripInfoPackage.rtcTime
                ,tripInfoPackage.deviceId);
    }
}

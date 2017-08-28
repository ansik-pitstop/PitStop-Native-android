package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.RetrievedTrip215;
import com.pitstop.models.Trip215End;
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
    private Handler handler;
    private TripInfoPackage tripInfoPackage;
    private Callback callback;

    public Trip215EndUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler handler) {
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
        device215TripRepository.retrieveLatestTrip(tripInfoPackage.deviceId, new Repository.Callback<RetrievedTrip215>() {
            @Override
            public void onSuccess(RetrievedTrip215 data) {

                //Latest trip does not exist, don't store trip end
                if (data == null || data.getTripIdRaw() != tripInfoPackage.tripId){
                    callback.onStartTripNotFound();
                    return;
                }
                //This trip has already has a trip end stored
                else if (data.isEnded()){
                    callback.onTripAlreadyEnded();
                    return;
                }
                //Another trip start got posted before onSuccess called, ignore trip end
                //Back-end should have the logic implemented to take care of this case
                else if (data.getMileage() > tripInfoPackage.mileage){
                    callback.onRealTimeTripEndSuccess();
                    return;
                }

                Trip215End tripEnd = convertToTrip215End(tripInfoPackage,data);

                //Store trip end
                device215TripRepository.storeTripEnd(tripEnd, new Repository.Callback() {
                    @Override
                    public void onSuccess(Object data) {

                        //Send notification if a real time update occurred
                        if (tripInfoPackage.rtcTime > tripInfoPackage.terminalRtcTime
                                - HISTORICAL_OFFSET){
                            callback.onRealTimeTripEndSuccess();

                        }
                        else{
                            callback.onHistoricalTripEndSuccess();
                        }
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"onError() error: "+error.getMessage());
                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            Log.d(TAG,"Storing trip locally due to error.");
                            device215TripRepository.storeTripLocally(tripInfoPackage);
                        }
                        callback.onError(error);
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
                callback.onError(error);
            }
        });
    }

    //If latest trip succeeded
    private Trip215End convertToTrip215End(TripInfoPackage tripInfoPackage, RetrievedTrip215 tripStart){
        double tripMileage = tripInfoPackage.mileage - tripStart.getMileage();

        return new Trip215End(tripStart.getTripId(), tripInfoPackage.tripId
                , tripInfoPackage.deviceId, tripInfoPackage.rtcTime, tripMileage);
    }
}

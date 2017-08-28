package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class HandlePidDataUseCaseImpl implements HandlePidDataUseCase {

    private final String TAG = getClass().getSimpleName();

    private PidRepository pidRepository;
    private Device215TripRepository tripRepository;
    private Handler handler;
    private PidPackage pidPackage;
    private Callback callback;

    public HandlePidDataUseCaseImpl(PidRepository pidRepository
            , Device215TripRepository tripRepository, Handler handler) {
        this.pidRepository = pidRepository;
        this.tripRepository = tripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(PidPackage pidPackage, Callback callback) {
        this.callback = callback;
        this.pidPackage = pidPackage;
        handler.post(this);
    }

    @Override
    public void run() {
        if (Device215TripRepository.getLocalLatestTripId() == -1){
            Log.d(TAG,"Repository latest trip id is -1, getting latest trip id from server.");
            tripRepository.retrieveLatestTrip(pidPackage.deviceId, new Repository.Callback<Trip215>() {
                @Override
                public void onSuccess(Trip215 data) {
                    if (data == null){
                        createTripUsingPid();
                        return;
                    }
                    Log.d(TAG,"Got latest trip id from server, id: "+pidPackage.tripId);
                    insertPid(data.getTripId());
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error getting latest trip id from server, creating trip using pid");
                    if (!error.getError().equals(RequestError.ERR_OFFLINE)){
                        createTripUsingPid();
                    }
                    else{
                        callback.onError(error);
                    }
                }
            });
        }
        else{
            Log.d(TAG,"Repository latest trip id is "+Device215TripRepository.getLocalLatestTripId()
                    +", inserting PID");
            insertPid(Device215TripRepository.getLocalLatestTripId());
        }

    }

    private void insertPid(int tripId){
        Log.d(TAG,"insertPid()");
        pidRepository.insertPid(pidPackage, tripId, new Repository.Callback<Object>() {
            @Override
            public void onSuccess(Object response){
                Log.d(TAG,"successfully added pids");
                callback.onSuccess();
            }

            @Override
            public void onError(RequestError error){
                Log.d(TAG,"insertPid() onError, message: "+error.getMessage());
                //Check whether its a "trip not found" error
                if (error.getMessage().contains("not found")){
                    createTripUsingPid();
                }
                else{
                    callback.onError(error);
                }
            }
        });
    }

    private void createTripUsingPid(){
        Log.d(TAG,"createTripUsingPid()");
        tripRepository.storeTripStart(pidPackageToTrip215Start(pidPackage)
                , new Repository.Callback<Trip215>() {

                    @Override
                    public void onSuccess(Trip215 data) {

                        Log.d(TAG,"Stored trip start using PID. Attempting to store pid again!");
                        pidRepository.insertPid(pidPackage,data.getTripId(), new Repository.Callback<Object>() {

                            @Override
                            public void onSuccess(Object response){
                                Log.d(TAG,"Success storing PID after trip start was stored.");
                                callback.onSuccess();
                            }

                            @Override
                            public void onError(RequestError error){
                                Log.d(TAG,"Error storing PID even after trip start was saved.");
                                callback.onError(error);
                            }
                        });
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"Error saving trip start using PID! error: "+error);
                        if (!error.getError().equals(RequestError.ERR_OFFLINE)){
                            run(); //Try again, it seems that trip start exists
                        }
                        else{
                            callback.onError(error);
                        }
                    }
                });
    }

    private Trip215 pidPackageToTrip215Start(PidPackage pidPackage){
        long tripIdRaw;
        try{
             tripIdRaw = Long.valueOf(pidPackage.tripId);
        }catch(NumberFormatException e){
            e.printStackTrace();
            tripIdRaw = -1;
        }
        double mileage;
        try{
            mileage = Double.valueOf(pidPackage.tripMileage);
        }catch (NumberFormatException e){
            e.printStackTrace();
            mileage = 0;
        }
        long rtcTime;
        try{
            rtcTime = Long.valueOf(pidPackage.rtcTime);
        }catch (NumberFormatException e){
            e.printStackTrace();
            rtcTime = 0;
        }

        return new Trip215(Trip215.TRIP_START,tripIdRaw,mileage,rtcTime,pidPackage.deviceId);
    }
}

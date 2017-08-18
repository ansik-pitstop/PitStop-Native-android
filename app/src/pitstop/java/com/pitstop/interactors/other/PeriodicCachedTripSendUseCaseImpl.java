package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.utils.ConnectionChecker;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public class PeriodicCachedTripSendUseCaseImpl implements PeriodicCachedTripSendUseCase {

    private final String TAG = getClass().getSimpleName();
    private final int SEND_CACHED_TRIP_INTERVAL = 60000; //Send trips once a minute

    private Device215TripRepository device215TripRepository;
    private UseCaseComponent useCaseComponent;
    private ConnectionChecker connectionChecker;
    private Handler handler;

    private static boolean cachedTripSenderActive = false;

    public PeriodicCachedTripSendUseCaseImpl(Device215TripRepository device215TripRepository
            , ConnectionChecker connectionChecker, Handler handler){

        this.device215TripRepository = device215TripRepository;
        this.connectionChecker = connectionChecker;
        this.handler = handler;
    }

    @Override
    public void execute(UseCaseComponent useCaseComponent) {
        this.useCaseComponent = useCaseComponent;
        //Don't allow more than one instance
        if (!cachedTripSenderActive){
            cachedTripSenderActive = true;
            handler.post(this);
        }
    }

    @Override
    public void run() {
        Log.d(TAG,"periodicCachedTripSender executing. Connection Status: "
                +connectionChecker.isConnected()+", locally stored trips size: "
                +device215TripRepository.getLocallyStoredTrips().size());
        if (!connectionChecker.isConnected()){
            handler.postDelayed(this,SEND_CACHED_TRIP_INTERVAL);
            return;
        }

        for (TripInfoPackage trip215: device215TripRepository.getLocallyStoredTrips()){
            if (trip215.flag == TripInfoPackage.TripFlag.START){
                //Execute trip start
                useCaseComponent.trip215StartUseCase().execute(trip215, new Trip215StartUseCase.Callback() {
                    @Override
                    public void onRealTimeTripStartSuccess() {
                        Log.d(TAG,"Success uploading Real-Time trip START local DB");
                    }

                    @Override
                    public void onHistoricalTripStartSuccess() {
                        Log.d(TAG,"Success uploading Historical trip START from local DB");
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"Error uploading Real-Time trip START from local DB");
                    }
                });
            }
            else if (trip215.flag == TripInfoPackage.TripFlag.END){
                useCaseComponent.trip215EndUseCase().execute(trip215, new Trip215EndUseCase.Callback() {
                    @Override
                    public void onHistoricalTripEndSuccess() {
                        Log.d(TAG,"Success uploading Historical trip END from local DB");
                    }

                    @Override
                    public void onRealTimeTripEndSuccess() {
                        Log.d(TAG,"Success uploading Real-Time trip END from local DB");
                    }

                    @Override
                    public void onStartTripNotFound() {
                        Log.d(TAG,"Error uploading trip END from local DB, no trip start found!");
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"Error uploading trip from local DB: "+error.getMessage());
                    }
                });
            }
        }
        device215TripRepository.removeLocallyStoredTrips();
        Log.d(TAG,"Removing all trips from local trip storage, size after removal: "
                +device215TripRepository.getLocallyStoredTrips().size());
        handler.postDelayed(this,SEND_CACHED_TRIP_INTERVAL);
    }
}

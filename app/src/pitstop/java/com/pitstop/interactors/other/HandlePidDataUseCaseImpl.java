package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.models.Pid;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.Repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class HandlePidDataUseCaseImpl implements HandlePidDataUseCase {

    private static final String TAG = HandlePidDataUseCaseImpl.class.getSimpleName();

    private static final int PID_CHUNK_SIZE = 5; //Todo: change back to 10
    private static final int SEND_INTERVAL = 300000; //Todo: change back to 5 minutes

    private PidRepository pidRepository;
    private Device215TripRepository tripRepository;
    private LocalPidStorage localPidStorage;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private PidPackage pidPackage;
    private Callback callback;

    private static Handler periodicHandler;
    private Runnable periodicPidDataSender = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Sending pid data to server periodically.");
            if (Device215TripRepository.getLocalLatestTripId() == -1){
                insertPidData(Device215TripRepository.getLocalLatestTripId());
            }
            periodicHandler.postDelayed(this,SEND_INTERVAL);
        }
    };

    public HandlePidDataUseCaseImpl(PidRepository pidRepository
            , Device215TripRepository tripRepository, LocalPidStorage localPidStorage
            , Handler useCaseHandler, Handler mainHandler) {
        this.localPidStorage = localPidStorage;
        this.pidRepository = pidRepository;
        this.tripRepository = tripRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(PidPackage pidPackage, Callback callback) {
        this.callback = callback;
        this.pidPackage = pidPackage;

        //This will only happen for one use case execution
        if (periodicHandler == null){
            periodicHandler = new Handler();
            periodicHandler.post(periodicPidDataSender);
        }

        useCaseHandler.post(this);
    }

    private void onSuccess(){
        mainHandler.post(() -> callback.onSuccess());
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        localPidStorage.createPIDData(getPidDataObject(pidPackage));
        if(localPidStorage.getPidDataEntryCount() < PID_CHUNK_SIZE) {
            return;
        }

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
                    insertPidData(data.getTripId());
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error getting latest trip id from server, creating trip using pid");
                    if (!error.getError().equals(RequestError.ERR_OFFLINE)){
                        createTripUsingPid();
                    }
                    else{
                        HandlePidDataUseCaseImpl.this.onError(error);
                    }
                }
            });
        }
        else{
            Log.d(TAG,"Repository latest trip id is "+Device215TripRepository.getLocalLatestTripId()
                    +", inserting PID");
            insertPidData(Device215TripRepository.getLocalLatestTripId());
        }

    }

    private void createTripUsingPid(){
        Log.d(TAG,"createTripUsingPid()");
        tripRepository.storeTripStart(pidPackageToTrip215Start(pidPackage)
            , new Repository.Callback<Trip215>() {

                @Override
                public void onSuccess(Trip215 data) {

                    Log.d(TAG,"Stored trip start using PID. Attempting to store pid again!");
                    insertPidData(data.getTripId());
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error saving trip start using PID! error: "+error);
                    HandlePidDataUseCaseImpl.this.onError(error);
                }
            });
    }

    //Set trip id and seperate pids into chunks
    private void insertPidData(int tripId){
        List<Pid> allPids = localPidStorage.getAllPidDataEntries();

        int counter = 0;
        List<Pid> chunk = new ArrayList<>();
        for (Pid p: allPids){
            p.setTripId(tripId);
            chunk.add(p);
            counter ++;

            if (counter == PID_CHUNK_SIZE){
                counter = 0;
                pidRepository.insertPid(chunk, new Repository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object response){
                        Log.d(TAG,"PIDS added!");
                        HandlePidDataUseCaseImpl.this.onSuccess();
                        localPidStorage.deleteAllPidDataEntries();
                    }
                    @Override
                    public void onError(RequestError error){
                        Log.d(TAG,"error adding PIDS");
                        HandlePidDataUseCaseImpl.this.onError(error);
                        if (localPidStorage.getAllPidDataEntries().size() > 5000){
                            localPidStorage.deleteAllPidDataEntries();
                        }
                    }
                });
                chunk = new ArrayList<>();
            }
        }
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

    private Pid getPidDataObject(PidPackage pidPackage){

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        pidDataObject.setCalculatedMileage(0);
        try{
            pidDataObject.setMileage(Double.parseDouble(pidPackage.tripMileage));
        }catch(NumberFormatException e){
            pidDataObject.setMileage(0);
        }

        pidDataObject.setDataNumber("");
        pidDataObject.setTripIdRaw(Long.parseLong(pidPackage.tripId));
        pidDataObject.setTripId(-1);
        pidDataObject.setRtcTime(pidPackage.rtcTime);
        pidDataObject.setDeviceId(pidPackage.deviceId);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> pidEntry : pidPackage.pids.entrySet()) {
            sb.append(pidEntry.getKey());
            sb.append(": ");
            sb.append(pidEntry.getValue());
            sb.append(" / ");
            try {
                JSONObject pid = new JSONObject();
                pid.put("id", pidEntry.getKey());
                pid.put("data", pidEntry.getValue());
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "PIDs received: " + sb.toString());

        pidDataObject.setPids(pids.toString());

        return pidDataObject;
    }
}

package com.pitstop.repositories;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pitstop.R.array.pid;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class PidRepository implements Repository{

    private final String TAG = getClass().getSimpleName();
    private static final int PID_CHUNK_SIZE = 5; //Todo: change back to 10
    private static final int SEND_INTERVAL = 300000; //Todo: change back to 5 minutes

    private NetworkHelper networkHelper;
    private LocalPidStorage localPidStorage;

    private Handler handler = new Handler();
    private Runnable periodicPidDataSender = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Sending pid data to server periodically.");
            sendPidDataToServer(null);
            new Handler().postDelayed(this,SEND_INTERVAL);
        }
    };

    public PidRepository(NetworkHelper networkHelper, LocalPidStorage localPidStorage) {
        this.networkHelper = networkHelper;
        this.localPidStorage = localPidStorage;
        //Send pid data every 5 minutes regardless of chunk size
        handler.post(periodicPidDataSender);
    }

    public void insertPid(List<Pid> pids, Callback<Object> callback){
        Log.d(TAG,"insertPid() locally stored pid count: "+localPidStorage.getPidDataEntryCount()
                +", pid: "+pid);

        if (pids == null || pids.isEmpty()){
            callback.onError(RequestError.getUnknownError());
            return;
        }

        JSONArray pidArray = new JSONArray();
        for (Pid p: pids){
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum", p.getDataNumber());
                jsonObject.put("rtcTime", Long.parseLong(p.getRtcTime()));
                jsonObject.put("tripMileage", p.getMileage());
                jsonObject.put("tripIdRaw", p.getTripIdRaw());
                jsonObject.put("calculatedMileage", p.getCalculatedMileage());
                jsonObject.put("pids", new JSONArray(p.getPids()));
                pidArray.put(jsonObject);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        JSONObject body = new JSONObject();

        try {
            body.put("tripId", pids.get(0).getTripId());
            body.put("scannerId", pids.get(0).getDeviceId());
            body.put("pidArray", pidArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.postNoAuth("scan/pids", (response, requestError) -> {
            if (requestError == null) {
                Log.i(TAG, "PIDS saved");
                callback.onSuccess(response);
            }
            else{
                Log.e(TAG, "save pid error: " + requestError.getMessage());
                if (callback != null){
                    callback.onError(requestError);
                }
            }
        }, body);
    }

    private synchronized void sendPidDataToServer(@Nullable Callback callback){

        List<Pid> pidDataEntries = localPidStorage.getAllPidDataEntries();
        Log.d(TAG,"sendPidDataToServer() pidDatEntries.size() : "+pidDataEntries.size());
        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks+1];
        List<Integer> tripIdList = new ArrayList<>();
        List<String> deviceIdList = new ArrayList<>();

        int counter = 0;
        int arrCounter = 0;
        JSONArray pidArray = new JSONArray();
        for (Pid pidDataObject: pidDataEntries){
            if (counter == 0) pidArrays[arrCounter] = pidArray;
            try{
                tripIdList.add(pidDataObject.getTripId());
                deviceIdList.add(pidDataObject.getDeviceId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum", pidDataObject.getDataNumber());
                jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                jsonObject.put("tripMileage", pidDataObject.getMileage());
                jsonObject.put("tripIdRaw", pidDataObject.getTripIdRaw());
                jsonObject.put("calculatedMileage", pidDataObject.getCalculatedMileage());
                jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                pidArray.put(jsonObject);
            }catch(JSONException e){
                e.printStackTrace();
            }

            counter++;
            if (counter >= PID_CHUNK_SIZE){
                counter = 0;
                arrCounter++;
                pidArray = new JSONArray();
            }

        }

        int currentChunk = 0;
        for(JSONArray pids : pidArrays) {
            if(pids == null || pids.length() == 0 ) {
                continue;
            }

            JSONObject body = new JSONObject();

            try {
                body.put("tripId", tripIdList.get(currentChunk));
                body.put("scannerId", deviceIdList.get(currentChunk));
                body.put("pidArray", pids);
            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
            }

            networkHelper.postNoAuth("scan/pids", (response, requestError) -> {
                if (requestError == null) {
                    Log.i(TAG, "PIDS saved");
                    localPidStorage.deleteAllPidDataEntries();
                }
                else{
                    Log.e(TAG, "save pid error: " + requestError.getMessage());
                    if (callback != null){
                        callback.onError(requestError);
                    }
                    if (localPidStorage.getAllPidDataEntries().size() > 10000){
                        localPidStorage.deleteAllPidDataEntries();
                    }
                }
            }, body);

            currentChunk++;
        }
    }

    private Pid getPidDataObject(PidPackage pidPackage, int tripId){

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
        pidDataObject.setTripId(tripId);
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

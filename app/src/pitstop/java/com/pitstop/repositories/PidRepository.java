package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.database.LocalPidAdapter;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class PidRepository implements Repository{

    private final String TAG = getClass().getSimpleName();
    private static final int PID_CHUNK_SIZE = 5;

    private NetworkHelper networkHelper;
    private LocalPidAdapter localPidStorage;

    public PidRepository(NetworkHelper networkHelper, LocalPidAdapter localPidStorage) {
        this.networkHelper = networkHelper;
        this.localPidStorage = localPidStorage;
    }

    public void insertPid(PidPackage pid, int tripId, Callback<Object> callback){
        Log.d(TAG,"insertPid() locally stored pid count: "+localPidStorage.getPidDataEntryCount()
                +", pid: "+pid);
        localPidStorage.createPIDData(getPidDataObject(pid,tripId));
        if(localPidStorage.getPidDataEntryCount() >= PID_CHUNK_SIZE
                && localPidStorage.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer(callback);
        }
        else{
            callback.onSuccess(null);
        }
    }

    private void sendPidDataToServer(Callback callback){

        List<Pid> pidDataEntries = localPidStorage.getAllPidDataEntries();
        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];
        List<Integer> tripIdList = new ArrayList<>();
        List<String> deviceIdList = new ArrayList<>();

        try {
            for(int chunkNumber = 0 ; chunkNumber < chunks ; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }

                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
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
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        int currentChunk = 0;
        for(JSONArray pids : pidArrays) {
            if(pids.length() == 0) {
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

            networkHelper.postNoAuth("scan/pids", new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        Log.i(TAG, "PIDS saved");
                        localPidStorage.deleteAllPidDataEntries();
                    }
                    else{
                        Log.e(TAG, "save pid error: " + requestError.getMessage());
                        callback.onError(requestError);
                        if (localPidStorage.getAllPidDataEntries().size() > 10000){
                            localPidStorage.deleteAllPidDataEntries();
                        }
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
        pidDataObject.setDataNumber("");  //FIX OR LOOK INTO TODO
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

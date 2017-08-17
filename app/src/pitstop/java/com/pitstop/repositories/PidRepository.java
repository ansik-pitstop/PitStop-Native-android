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
    private static final int PID_CHUNK_SIZE = 15;

    private final String ERR_NO_TRIP_FOUND = "error_no_trip_exists";

    private NetworkHelper networkHelper;
    private LocalPidAdapter localPidStorage;

    public PidRepository(NetworkHelper networkHelper, LocalPidAdapter localPidStorage) {
        this.networkHelper = networkHelper;
        this.localPidStorage = localPidStorage;
    }

    public void insertPid(PidPackage pid, Callback<Object> callback){
        localPidStorage.createPIDData(getPidDataObject(pid));
        if(localPidStorage.getPidDataEntryCount() >= PID_CHUNK_SIZE
                && localPidStorage.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
            sendPidDataToServer(callback);
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
                    tripIdList.add((int)pidDataObject.getTripId());
                    deviceIdList.add(pidDataObject.getDeviceId());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("tripMileage", pidDataObject.getMileage());
                    jsonObject.put("tripIdRaw", pidDataObject.getTripId());
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

    private Pid getPidDataObject(PidPackage pidPackage){

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        //Car car = localCarStorage.getCarByScanner(deviceId);

        double mileage;
        double calculatedMileage;

//        //TODO: Ask Nitish what's going on below
//        if(pidPackage.tripMileage != null && !pidPackage.tripMileage.isEmpty()) {
//            mileage = Double.parseDouble(pidPackage.tripMileage) / 1000;
//            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
//        }
//
//        //FIX OR LOOK INTO
//// } else if(lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
////            mileage = Double.parseDouble(lastData.tripMileage)/1000;
////            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
////        }
//        else {
//            mileage = 0;
//            calculatedMileage = 0;
//        }

      //  pidDataObject.setMileage(mileage); // trip mileage from device
      //  pidDataObject.setCalculatedMileage(calculatedMileage);
        pidDataObject.setDataNumber("");  //FIX OR LOOK INTO
        pidDataObject.setTripId(Long.parseLong(pidPackage.tripId));
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

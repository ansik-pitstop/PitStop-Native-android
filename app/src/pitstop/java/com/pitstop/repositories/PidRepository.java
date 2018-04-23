package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.database.LocalPidStorage;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class PidRepository implements Repository{

    private final String TAG = getClass().getSimpleName();

    private NetworkHelper networkHelper;

    public PidRepository(NetworkHelper networkHelper, LocalPidStorage localPidStorage) {
        this.networkHelper = networkHelper;
    }

    public void insertPid(List<Pid> pids, Callback<List<Pid>> callback){
        Log.d(TAG,"insertPid() pids: "+pids);

        if (pids == null || pids.isEmpty()){
            callback.onError(RequestError.getUnknownError());
            return;
        }

        JSONArray pidArray = new JSONArray();
        for (Pid p: pids){
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dataNum", p.getDataNumber());
                jsonObject.put("bluetoothDeviceTime", Long.parseLong(p.getRtcTime()));
                jsonObject.put("tripMileage", p.getMileage());
                jsonObject.put("tripIdRaw", p.getTripIdRaw());
                jsonObject.put("calculatedMileage", p.getCalculatedMileage());
                jsonObject.put("pids", new JSONArray(p.getPids()));
                pidArray.put(jsonObject);
            }catch(JSONException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            }
        }

        JSONObject body = new JSONObject();

        try {
            body.put("tripId", pids.get(0).getTripId());
            body.put("scannerId", pids.get(0).getDeviceId());
            body.put("pidArray", pidArray);
        } catch (JSONException e) {
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
        }

        networkHelper.post("scan/pids", (response, requestError) -> {
            if (requestError == null) {
                Log.i(TAG, "PIDS saved");
                callback.onSuccess(pids);
            }
            else{
                Log.e(TAG, "save pid error: " + requestError.getMessage());
                if (callback != null){
                    callback.onError(requestError);
                }
            }
        }, body);
    }

}

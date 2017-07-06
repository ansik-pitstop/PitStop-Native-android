package com.pitstop.repositories;

import com.pitstop.models.Trip215;
import com.pitstop.models.TripStart;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Device215TripRepository implements Repository{

    private final String SCAN_END_POINT = "scan/trip";
    private NetworkHelper networkHelper;

    public Device215TripRepository(NetworkHelper networkHelper){
        this.networkHelper = networkHelper;
    }

    public void storeTripStart(Trip215 tripStart, Callback<TripStart> callback){

        JSONObject body = new JSONObject();
        try {
            body.put("vin", tripStart.getVin());
            body.put("rtcTimeStart", tripStart.getRtcTime());
            body.put("tripIdRaw", tripStart.getId());
            body.put("mileageStart", tripStart.getMileage());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.postNoAuth(SCAN_END_POINT, getStoreTripStartRequestCallback(callback), body);
    }

    private RequestCallback getStoreTripStartRequestCallback(Callback callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    callback.onSuccess(null);
                }
            }
        };

        return requestCallback;
    }

    public void storeTripEnd(Trip215 tripEnd, Callback callback){
        JSONObject body = new JSONObject();

        try {
            body.put("mileage", Double.parseDouble(mileage) / 1000);
            body.put("tripId", tripId);
            body.put("rtcTimeEnd", Long.parseLong(rtcTime));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.putNoAuth(SCAN_END_POINT, getStoreTripRequestCallback(callback), body);
    }

    private RequestCallback getStoreTripRequestCallback(Callback callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    callback.onSuccess(null);
                }
            }
        };

        return requestCallback;
    }
}

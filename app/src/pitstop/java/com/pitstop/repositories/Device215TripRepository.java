package com.pitstop.repositories;

import com.pitstop.models.Trip215;
import com.pitstop.models.TripEnd;
import com.pitstop.models.TripStart;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import static com.pitstop.R.id.mileage;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Device215TripRepository implements Repository{

    private NetworkHelper networkHelper;

    public Device215TripRepository(NetworkHelper networkHelper){
        this.networkHelper = networkHelper;
    }

    public void storeTripStart(Trip215 tripStart, Callback<TripStart> callback){

        JSONObject body = new JSONObject();
        try {
            body.put("scannerId", scannerId);
            body.put("rtcTimeStart", Long.parseLong(rtcTime));
            body.put("tripIdRaw", tripIdRaw);
            if (mileage != null)
                body.put("mileageStart", mileage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.postNoAuth("scan/trip", callback, body);
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

    public void storeTripEnd(Trip215 tripEnd){

    }
}

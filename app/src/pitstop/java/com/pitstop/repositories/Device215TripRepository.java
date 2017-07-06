package com.pitstop.repositories;

import com.pitstop.models.Trip215;
import com.pitstop.models.TripStart;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stores/Retrieves device trip start/end remotely
 *
 * Created by Karol Zdebel on 7/6/2017.
 */

public class Device215TripRepository implements Repository{

    private final String SCAN_END_POINT = "scan/trip";
    private final String LATEST_TRIP_QUERY = "/?vin=%s&latest=true&active=true";
    private NetworkHelper networkHelper;

    public Device215TripRepository(NetworkHelper networkHelper){
        this.networkHelper = networkHelper;
    }

    public void storeTripStart(Trip215 tripStart, Callback<TripStart> callback){

        JSONObject body = new JSONObject();
        try {
            body.put("scannerId", tripStart.getScannerName());
            body.put("rtcTimeStart", tripStart.getRtcTime());
            body.put("tripIdRaw", tripStart.getTripId());
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
            body.put("mileage", tripEnd.getMileage());
            body.put("tripId", tripEnd.getTripId());
            body.put("rtcTimeEnd", tripEnd.getRtcTime());
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

    public void retrieveLatestTrip(String scannerName, Callback<Trip215> callback){
        networkHelper.get(String.format(SCAN_END_POINT+LATEST_TRIP_QUERY,scannerName)
                ,getRetrieveLatestTripCallback(callback,scannerName));
    }

    private RequestCallback getRetrieveLatestTripCallback(Callback callback, String scannerName){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject data = new JSONObject(response);
                        int id = data.getInt("id");
                        int mileage = data.getInt("mileage");
                        int rtcTime = data.getInt("rtcTimeStart");
                        Trip215 trip = new Trip215(id,mileage,rtcTime,scannerName);
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }
                    callback.onSuccess(null);
                }
            }
        };

        return requestCallback;
    }
}

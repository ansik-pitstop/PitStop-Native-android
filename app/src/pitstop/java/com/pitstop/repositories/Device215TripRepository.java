package com.pitstop.repositories;

import android.os.Handler;
import android.util.Log;

import com.pitstop.database.LocalDeviceTripStorage;
import com.pitstop.models.Trip215;
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

    private final String TAG = getClass().getSimpleName();
    private final String SCAN_END_POINT = "scan/trip";
    private final String LATEST_TRIP_QUERY = "/?scannerId=%s&latest=true&active=true";
    private NetworkHelper networkHelper;
    private LocalDeviceTripStorage localDeviceTripStorage;

    private Handler handler = new Handler();
    private final int SEND_CACHED_TRIP_INTERVAL = 60000; //Send trips once a minute
    private Runnable periodicCachedTripSender = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"periodicCachedTripSender executing. Connection Status: "
                    +networkHelper.isConnected()+", locally stored trips size: "
                    +localDeviceTripStorage.getAllTrips().size());
            if (!networkHelper.isConnected()) return;

            for (Trip215 trip215: localDeviceTripStorage.getAllTrips()){
                if (trip215.getTripId() == -1){
                    storeTripStart(trip215, new Callback<Trip215>() {
                        @Override
                        public void onSuccess(Trip215 data) {
                            Log.d(TAG,"successfully sent trip START to server from local storage!");
                        }

                        @Override
                        public void onError(RequestError error) {
                            Log.d(TAG,"failed to send trip START to server from local storage!");
                        }
                    });
                }
                else{
                    storeTripEnd(trip215,new Callback<Trip215>() {
                        @Override
                        public void onSuccess(Trip215 data) {
                            Log.d(TAG,"successfully sent trip END to server from local storage!");
                        }

                        @Override
                        public void onError(RequestError error) {
                            Log.d(TAG,"failed to send trip END to server from local storage!");
                        }
                    });
                }
            }
            localDeviceTripStorage.removeAllTrips();
            Log.d(TAG,"Removing all trips from local trip storage, size after removal: "
                    +localDeviceTripStorage.getAllTrips().size());
            handler.postDelayed(this,SEND_CACHED_TRIP_INTERVAL);
        }
    };

    public static int localLatestTripId = -1;

    public Device215TripRepository(NetworkHelper networkHelper
            , LocalDeviceTripStorage localDeviceTripStorage){

        this.networkHelper = networkHelper;
        this.localDeviceTripStorage = localDeviceTripStorage;
        handler.post(periodicCachedTripSender);
    }

    public void storeTripStart(Trip215 tripStart, Callback<Trip215> callback){

        JSONObject body = new JSONObject();
        try {
            body.put("scannerId", tripStart.getScannerName());
            body.put("rtcTimeStart", tripStart.getRtcTime());
            body.put("tripIdRaw", String.valueOf(tripStart.getTripIdRaw()));
            body.put("mileageStart", tripStart.getMileage());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.postNoAuth(SCAN_END_POINT, getStoreTripStartRequestCallback(
                callback,tripStart), body);
    }

    private RequestCallback getStoreTripStartRequestCallback(Callback<Trip215> callback
            , Trip215 trip215){

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    callback.onSuccess(null);
                    try {
                        JSONObject data = new JSONObject(response);
                        localLatestTripId = data.getInt("id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Log.d(TAG,"Error storing trip start, message: "+requestError.getMessage());
                    if (requestError.getError().equals(RequestError.ERR_OFFLINE)){
                        Log.d(TAG,"Storing trip locally due to OFFLINE error");
                        localDeviceTripStorage.storeDeviceTrip(trip215);
                        Log.d(TAG,"Local storage size after adding trip: "
                                +localDeviceTripStorage.getAllTrips().size());
                    }
                    callback.onError(requestError);
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

        networkHelper.putNoAuth(SCAN_END_POINT, getStoreTripEndRequestCallback(
                callback,tripEnd), body);
    }

    private RequestCallback getStoreTripEndRequestCallback(Callback callback, Trip215 trip){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    callback.onSuccess(null);
                }
                else{
                    Log.d(TAG,"Error storing trip end, message: "+requestError.getMessage());
                    if (requestError.getError().equals(RequestError.ERR_OFFLINE)){
                        Log.d(TAG,"Storing trip locally due to OFFLINE error");
                        localDeviceTripStorage.storeDeviceTrip(trip);
                        Log.d(TAG,"Local storage size after adding trip: "
                                +localDeviceTripStorage.getAllTrips().size());
                    }
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void retrieveLatestTrip(String scannerName, Callback<Trip215> callback){
        networkHelper.get(String.format(SCAN_END_POINT+LATEST_TRIP_QUERY,scannerName)
                ,getRetrieveLatestTripCallback(callback,scannerName));
    }

    private RequestCallback getRetrieveLatestTripCallback(Callback<Trip215> callback, String scannerName){
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject data = new JSONObject(response);
                        //Trip start didn't make it to backend
                        if (!data.has("id")){
                            callback.onSuccess(null);
                            return;
                        }
                        int id = data.getInt("id");
                        localLatestTripId = id;
                        long tripIdRaw = data.getLong("tripIdRaw");
                        double mileage = data.getDouble("mileageStart");
                        int rtcTime = data.getInt("rtcTimeStart");
                        Trip215 trip = new Trip215(id,tripIdRaw,mileage,rtcTime,scannerName);
                        callback.onSuccess(trip);
                    }
                    catch(JSONException e){
                        callback.onError(requestError);
                        e.printStackTrace();
                    }

                }
                else{
                    callback.onError(requestError);
                }
            }
        };
    }
}

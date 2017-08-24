package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalDeviceTripStorage;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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

    public static int localLatestTripId = -1;
    private String storedDeviceId = "";

    public Device215TripRepository(NetworkHelper networkHelper
            , LocalDeviceTripStorage localDeviceTripStorage){

        this.networkHelper = networkHelper;
        this.localDeviceTripStorage = localDeviceTripStorage;
    }

    public void storeTripLocally(TripInfoPackage trip){
        localDeviceTripStorage.storeDeviceTrip(trip);
    }

    public List<TripInfoPackage> getLocallyStoredTrips(){
        return localDeviceTripStorage.getAllTrips();
    }

    public void removeLocallyStoredTrips(){
        localDeviceTripStorage.removeAllTrips();
    }


    public void storeTripStart(Trip215 tripStart, Callback<Trip215> callback){
        Log.d(TAG,"storeTripStart trip:"+tripStart);
        checkLatestTripIdNeedsReset(tripStart.getScannerName());

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
            , Trip215 trip){

        return (response, requestError) -> {
            if (requestError == null){
                try {
                    JSONObject data = new JSONObject(response);

                    localLatestTripId = data.getInt("id");
                    int tripId = data.getInt("id");
                    double mileage = data.getDouble("mileage_start");
                    long rtc = data.getLong("rtc_time_start");
                    String scannerName = trip.getScannerName();
                    long tripIdRaw = data.getLong("trip_id_raw");

                    Trip215 start = new Trip215(Trip215.TRIP_START,tripId,tripIdRaw,mileage,rtc,scannerName);
                    Log.d(TAG,"returning trip start: "+start);
                    callback.onSuccess(start);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onError(RequestError.getUnknownError());
                }
            }
            else{
                callback.onError(requestError);
            }
        };
    }

    public void storeTripEnd(Trip215 tripEnd, Callback callback){
        Log.d(TAG,"storeTripEnd trip:"+tripEnd);
        checkLatestTripIdNeedsReset(tripEnd.getScannerName());
        JSONObject body = new JSONObject();

        try {
            body.put("mileage", tripEnd.getMileage());
            body.put("tripId", tripEnd.getTripId());
            body.put("rtcTimeEnd", tripEnd.getRtcTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.putNoAuth(SCAN_END_POINT, getStoreTripEndRequestCallback(
                callback), body);
    }

    private RequestCallback getStoreTripEndRequestCallback(Callback callback){
        return (response, requestError) -> {
            if (requestError == null){
                callback.onSuccess(null);
            }
            else{
                callback.onError(requestError);
            }
        };
    }

    public void retrieveLatestTrip(String scannerName, Callback<Trip215> callback){
        checkLatestTripIdNeedsReset(scannerName);
        networkHelper.get(String.format(SCAN_END_POINT+LATEST_TRIP_QUERY,scannerName)
                ,getRetrieveLatestTripCallback(callback,scannerName));
    }

    private RequestCallback getRetrieveLatestTripCallback(Callback<Trip215> callback, String scannerName){
        return (response, requestError) -> {
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
                    Trip215 trip = new Trip215(Trip215.TRIP_START,id,tripIdRaw,mileage
                            ,rtcTime,scannerName);
                    callback.onSuccess(trip);
                }
                catch(JSONException e){
                    callback.onError(RequestError.getUnknownError());
                    e.printStackTrace();
                }

            }
            else{
                callback.onError(requestError);
            }
        };
    }

    /*Reset localLatestTripId if the scanner name changed, this should ideally be moved to
     someplace else where we can be certain a device change occurred*/
    private void checkLatestTripIdNeedsReset(String scannerName){
        if (!storedDeviceId.isEmpty() && scannerName != null && !scannerName.isEmpty()
                && !scannerName.equals(storedDeviceId)){
            Log.d(TAG,"Resetting local latest trip id!");
            storedDeviceId = scannerName;
            localLatestTripId = -1;
        }
    }
}

package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.database.LocalScannerStorage;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class ScannerRepository implements Repository {

    private final String TAG = getClass().getSimpleName();

    private NetworkHelper networkHelper;
    private LocalScannerStorage localScannerStorage;

    public ScannerRepository(NetworkHelper networkHelper, LocalScannerStorage localScannerStorage){
        this.networkHelper = networkHelper;
        this.localScannerStorage = localScannerStorage;
    }

    public void createScanner(ObdScanner scanner, Callback callback){
        Log.d(TAG,"creating scanner: "+scanner);
        putScanner(scanner,getCreateScannerCallback(callback,scanner));
    }

    private void putScanner(ObdScanner scanner, RequestCallback callback){
        JSONObject body = new JSONObject();

        try {
            body.put("carId", scanner.getCarId());
            body.put("scannerId", scanner.getScannerId());

            boolean isActive;
            if (scanner.getStatus() == null){
                isActive = false;
            }
            else{
                isActive = scanner.getStatus();
            }

            body.put("isActive", isActive);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"PUT scanner, body= "+body.toString());
        networkHelper.put("scanner", callback, body);
    }

    private RequestCallback getCreateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                Log.d(TAG,"Create scanner response: "+response+", error: "+requestError);
                if (requestError == null){
                    localScannerStorage.storeScanner(scanner);
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError);
                }
            }
        };
    }

    public void updateScanner(ObdScanner scanner, Callback<Object> callback){

        //Same logic for both
        Log.d(TAG,"updating scanner: "+scanner);
        putScanner(scanner,getUpdateScannerCallback(callback,scanner));
    }

    private RequestCallback getUpdateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                Log.d(TAG,"Update scanner response: "+response+", error: "+requestError);
                if (requestError == null){
                    localScannerStorage.updateScanner(scanner);
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError);
                }
            }
        };
    }

    /*boolean active: whether you want to look for active device or not*/
    public void getScanner(String scannerId, Callback<ObdScanner> callback){
        Log.d(TAG,"getting scanner, scannerId: "+scannerId);
        networkHelper.get("scanner/"+scannerId, getGetScannerCallback(callback));
    }

    private RequestCallback getGetScannerCallback(Callback<ObdScanner> callback){
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                Log.d(TAG,"Get scanner response: "+response+", error: "+requestError);
                if (requestError == null){
                    try{
                        //Data is returned in an array, first index is the scanner
                        JSONArray dataArray = new JSONArray(response);

                        //Check if scanner exists
                        if (dataArray.length() == 0){
                            callback.onSuccess(null);
                            return;
                        }

                        //GeFt scanner data
                        JSONObject data = (new JSONArray(response)).getJSONObject(0);

                        int carId = data.getInt("carId");
                        String deviceName = data.getString("scannerId");
                        String scannerId = data.getString("scannerId");
                        Boolean isActive = data.getBoolean("active");

                        ObdScanner obdScanner = new ObdScanner(carId,deviceName,scannerId);
                        obdScanner.setStatus(isActive);

                        localScannerStorage.removeScanner(obdScanner.getCarId());
                        localScannerStorage.storeScanner(obdScanner);
                        callback.onSuccess(obdScanner);
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

    public void deviceClockSync(@NotNull Long rtcTime, @NotNull String deviceId
            , @NotNull String vin, @NotNull String deviceType, Callback<String> callback) {

        Log.d(TAG,"deviceClockSync() rtcTime: "+rtcTime+", deviceId: "+deviceId
                +", vin: "+vin+", deviceType: "+deviceType);
        JSONObject body = new JSONObject();
        try{
            body.put("rtcTime",rtcTime);
            body.put("deviceId",deviceId);
            body.put("vin",vin);
            body.put("deviceType",deviceType);
            networkHelper.post("v1/device-clock-sync",(response, requestError) -> {
                if (requestError == null){
                    Log.d(TAG,"deviceClockSync() success response: "+response);
                    callback.onSuccess(response);
                }
                else{
                    Log.d(TAG,"deviceClockSync() error: "+requestError.getMessage());
                    callback.onError(requestError);
                }
            },body);
        }catch(JSONException e){
            e.printStackTrace();
            callback.onError(RequestError.getUnknownError());
        }

    }
}

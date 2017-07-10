package com.pitstop.repositories;

import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class ScannerRepository implements Repository {

    private NetworkHelper networkHelper;
    private LocalScannerAdapter localScannerStorage;

    public ScannerRepository(NetworkHelper networkHelper, LocalScannerAdapter localScannerStorage){
        this.networkHelper = networkHelper;
        this.localScannerStorage = localScannerStorage;
    }

    public void createScanner(ObdScanner scanner, Callback<Object> callback){
        putScanner(scanner,getCreateScannerCallback(callback,scanner));
    }

    private void putScanner(ObdScanner scanner, RequestCallback callback){
        JSONObject body = new JSONObject();

        try {
            body.put("carId", scanner.getCarId());
            body.put("scannerId", scanner.getScannerId());
            body.put("isActive", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.put("scanner", callback, body);
    }

    public RequestCallback getCreateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    localScannerStorage.storeScanner(scanner);
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError.getStatusCode());
                }
            }
        };

        return requestCallback;
    }

    public void updateScanner(ObdScanner scanner, Callback<Object> callback){
        //Same logic for both
        putScanner(scanner,getUpdateScannerCallback(callback,scanner));
    }

    public RequestCallback getUpdateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    localScannerStorage.updateScanner(scanner);
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError.getStatusCode());
                }
            }
        };

        return requestCallback;
    }

    public void getScanner(int scannerId, Callback<ObdScanner> callback){
        networkHelper.get("scanner/"+scannerId,getGetScannerCallback(callback));
    }

    public RequestCallback getGetScannerCallback(Callback<ObdScanner> callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject data = new JSONObject(response);

                        int carId = data.getInt("carId");
                        String deviceName = data.getString("scannerId");
                        String scannerId = data.getString("scannerId");

                        ObdScanner obdScanner = new ObdScanner(carId,deviceName,scannerId);
                        callback.onSuccess(obdScanner);
                    }
                    catch(JSONException e){
                        callback.onError(0);
                        e.printStackTrace();
                    }
                }
                else{
                    callback.onError(requestError.getStatusCode());
                }
            }
        };

        return  requestCallback;
    }

}

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

    public static final String ERR_NOT_EXISTS = "error_not_exists";

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

        networkHelper.put("scanner", callback, body);
    }

    private RequestCallback getCreateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        return new RequestCallback() {
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
    }

    public void updateScanner(ObdScanner scanner, Callback<Object> callback){
        //Same logic for both
        putScanner(scanner,getUpdateScannerCallback(callback,scanner));
    }

    private RequestCallback getUpdateScannerCallback(Callback<Object> callback, ObdScanner scanner) {
        return new RequestCallback() {
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
    }

    /*boolean active: whether you want to look for active device or not*/
    public void getScanner(String scannerId, boolean active, Callback<ObdScanner> callback){
        networkHelper.get("scanner/"+scannerId, getGetScannerCallback(callback));
    }

    private RequestCallback getGetScannerCallback(Callback<ObdScanner> callback){
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject data = new JSONObject(response);

                        int carId = data.getInt("carId");
                        String deviceName = data.getString("scannerId");
                        String scannerId = data.getString("scannerId");
                        Boolean isActive = data.getBoolean("active");

                        ObdScanner obdScanner = new ObdScanner(carId,deviceName,scannerId);
                        obdScanner.setStatus(isActive);
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
    }

}

package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.pitstop.BuildConfig;
import com.pitstop.network.HttpRequest;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.network.RequestType;

import static com.pitstop.utils.LogUtils.LOGI;
import static com.pitstop.utils.LogUtils.LOGV;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static final String clientId = BuildConfig.CLIENT_ID;

    private Context context;

    private static String sUser;
    private static String sVIN;
    private static List<String> sFailures;

    public static void setUser(String user) {
        sUser = user;
    }

    public static void setVIN(String VIN) {
        sVIN = VIN;
    }

    public static void setFailures(List<String> failures) {
        sFailures = failures;
    }

    public static String getUser() {
        return sUser;
    }

    public static String getVIN() {
        return sVIN;
    }

    public static List<String> getFailures() {
        return sFailures;
    }

    public NetworkHelper(Context context) {
        this.context = context;
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void postNoAuth(String uri, RequestCallback callback, JSONObject body) { // for login, sign up, scans
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", clientId)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void putNoAuth(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", clientId)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.PUT)
                .context(context)
                .createRequest()
                .executeAsync();
    }

//    public void addNewDtc(int carId, double mileage, String rtcTime, String dtcCode, boolean isPending,
//                          List<PIDInfo> freezeData, RequestCallback callback) {
//        LOGI(TAG, String.format("addNewDtc: carId: %s, mileage: %s," +
//                " rtcTime: %s, dtcCode: %s, isPending: %s", carId, mileage, rtcTime, dtcCode, isPending));
//
//        JSONObject body = new JSONObject();
//        JSONArray data = new JSONArray();
//
//        try {
//            for (PIDInfo info : freezeData) {
//                data.put(new JSONObject().put("id", info.pidType).put("data", info.value));
//            }
//
//            body.put("carId", carId);
//            body.put("issueType", CarIssue.DTC);
//            body.put("data",
//                    new JSONObject().put("mileage", mileage)
//                            .put("rtcTime", Long.parseLong(rtcTime))
//                            .put("dtcCode", dtcCode)
//                            .put("isPending", isPending));
//            //.put("freezeData", data));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        post("issue", callback, body);
//    }



    public void saveFreezeData(String scannerId, String serviceType, RequestCallback callback) {
        LOGI(TAG, String.format("saveFreezeData: scannerId: %s, serviceType: %s,", scannerId, serviceType));

        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("serviceType", serviceType);
            body.put("data", new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("scan/freezeData", callback, body);
    }

    public void sendTripStart(String scannerId, String rtcTime, String tripIdRaw, RequestCallback callback) {
        LOGI(TAG, String.format("sendTripStart: scannerId: %s, rtcTime: %s", scannerId, rtcTime));

        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("rtcTimeStart", Long.parseLong(rtcTime));
            body.put("tripIdRaw", tripIdRaw);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("scan/trip", callback, body);
    }

    public void saveTripMileage(int tripId, String mileage, String rtcTime, RequestCallback callback) {
        LOGI(TAG, String.format("saveTripMileage: tripId: %s," +
                " mileage: %s, rtcTime: %s", tripId, mileage, rtcTime));

        JSONObject tripBody = new JSONObject();

        try {
            tripBody.put("mileage", Double.parseDouble(mileage) / 1000);
            tripBody.put("tripId", tripId);
            tripBody.put("rtcTimeEnd", Long.parseLong(rtcTime));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        putNoAuth("scan/trip", callback, tripBody);
    }

    public void savePids(int tripId, String scannerId, JSONArray pidArr, RequestCallback callback) {
        LOGI(TAG, "savePids to " + scannerId);
        LOGV(TAG, "pidArr: " + pidArr.toString());

        JSONObject body = new JSONObject();

        try {
            body.put("tripId", tripId);
            body.put("scannerId", scannerId);
            body.put("pidArray", pidArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("scan/pids", callback, body);
    }

    /**
     *
     * @param tripId
     * @param failure
     * @param pidArr
     * @param callback
     */
    public void postTestPids(int tripId, ArrayList<String> failure,
                             JSONArray pidArr, RequestCallback callback){

        if (sVIN == null || sVIN.isEmpty() || sUser == null || sUser.isEmpty()){
            callback.done("User or Vin is not set", new RequestError());
        }

        JSONObject body = new JSONObject();
        JSONObject data = new JSONObject();

        try{
            data.put("user", sUser)
                    .put("tripId", tripId)
                    .put("vin", sVIN)
                    .put("failure", failure == null ? sFailures : failure)
                    .put("pidArray", pidArr);
            body.put("data", data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        postNoAuth("scan/test_pids", callback, body);
    }


}
package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

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

    private static final String clientId = BuildConfig.TEST_CLIENT_ID;

    private Context context;

    private static String sUser;
    private static String sVIN;
    private static List<String> sFailures;
    private static int sTripId;

    public static void setUser(String user) {
        sUser = user;
    }

    public static void setVIN(String VIN) {
        sVIN = VIN;
    }

    public static void setFailures(List<String> failures) {
        sFailures = failures;
    }

    public static void setTripId(int tripId) {
        sTripId = tripId;
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

    /**
     * @param pidArr
     * @param callback
     */
    public void postTestPids(JSONArray pidArr, RequestCallback callback) {

        if (sVIN == null || sVIN.isEmpty() || sUser == null || sUser.isEmpty() || sTripId == 0) {
            callback.done("User or Vin or Trip ID is not set", new RequestError());
            return;
        }

        JSONObject body = new JSONObject();

        JSONArray failures = new JSONArray();
        JSONObject fail = new JSONObject();

        try {
            for (String failure : sFailures) {
                failures.put(failure);
            }
            fail.put("fail", failures);

            body.put("user", sUser)
                    .put("tripId", sTripId)
                    .put("vin", sVIN)
                    .put("failure", fail)
                    .put("pidArray", pidArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, body.toString());

        postNoAuth("scan/test_pids", callback, body);
    }


    /**
     * @param pidArr
     * @param dtcString
     * @param callback
     */
    public void postTestDtcs(JSONArray pidArr, String dtcString, RequestCallback callback){
        if (sVIN == null || sVIN.isEmpty() || sUser == null || sUser.isEmpty()) {
            callback.done("User or Vin is not set", new RequestError());
            return;
        }

        JSONObject body = new JSONObject();

        JSONObject dtc = new JSONObject();
        JSONObject fail = new JSONObject();

        try {
            dtc.put("dtc", dtcString);
            fail.put("fail", dtc);

            body.put("user", sUser)
                    .put("tripId", sTripId)
                    .put("vin", sVIN)
                    .put("failure", fail)
                    .put("pidArray", pidArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Sending DTC");
        Log.d(TAG, body.toString());

        postNoAuth("scan/test_pids", callback, body);
    }

    public static void reset(){
        sTripId = 0;
        sFailures = null;
        sVIN = null;
    }


}
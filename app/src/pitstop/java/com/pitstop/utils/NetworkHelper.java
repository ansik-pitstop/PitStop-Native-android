package com.pitstop.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import com.parse.ParseInstallation;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.models.Trip;
import com.pitstop.network.HttpRequest;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.network.RequestType;
import com.pitstop.retrofit.PitstopAuthApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private final String CLIENT_ID;

    private static final String INSTALLATION_ID_KEY = "installationId";
    private Context context;
    private SharedPreferences sharedPreferences;
    private PitstopAuthApi pitstopAuthApi;

    public NetworkHelper(Context context, PitstopAuthApi pitstopAuthApi, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        CLIENT_ID = SecretUtils.getClientId(context);
        this.pitstopAuthApi = pitstopAuthApi;
    }

    private String getAccessToken() {
        return sharedPreferences.getString(PreferenceKeys.KEY_ACCESS_TOKEN, "");
    }

    public void getWithCustomUrl(String url, String uri, RequestCallback callback) {
         new HttpRequest.Builder()
                .url(url)
                .uri(uri)
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .context(context)
                .pitstopAuthApi(pitstopAuthApi)
                .createRequest()
                .executeAsync();
    }

    public void post(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + getAccessToken())
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .pitstopAuthApi(pitstopAuthApi)
                .createRequest()
                .executeAsync();
    }

    public void get(String uri, RequestCallback callback) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + getAccessToken())
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .context(context)
                .pitstopAuthApi(pitstopAuthApi)
                .createRequest()
                .executeAsync();
    }

    public void put(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + getAccessToken())
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.PUT)
                .pitstopAuthApi(pitstopAuthApi)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    /************The methods below are not to be referenced anymore ***********************/
    /************Please use repositories and interactors(use cases) instead ***************/

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public boolean isConnected(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void deleteUserCar(int carId, RequestCallback callback) {

        JSONObject body = new JSONObject();
        try {
            body.put("userId", 0);
            body.put("carId", carId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public void getShops(RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "getShops");
        get("shop", callback);
    }

    public void postFreezeFrame(FreezeFramePackage ffPackage, RequestCallback callback){
        Log.d(TAG, "Posting FF:" + ffPackage);
        JSONObject body = new JSONObject();
        JSONArray freezePidArray = new JSONArray();
        JSONArray pids = new JSONArray();
        Map<String, String> ff = ffPackage.freezeData;
        try {
            for (Map.Entry<String, String> entry : ff.entrySet()){
                pids.put(new JSONObject()
                        .put("id", entry.getKey())
                        .put("data", entry.getValue()));
            }
            freezePidArray.put(new JSONObject()
                    .put("rtcTime", ffPackage.rtcTime)
                    .put("pids", pids));
            body.put("scannerId", ffPackage.deviceId).put("freezePidArray", freezePidArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("scan/pids/freeze", callback, body);
    }

    /**
     * Get the aggregated settings
     *
     * @param userId
     * @param callback
     */
    public void getUserSettingsById(int userId, RequestCallback callback) {
        //GET /settings?userId=
       // Logger.getInstance().LOGI(TAG, "getUserSettingsById: " + userId);
        get("settings/?userId=" + userId, callback);
    }

    public void getUpcomingCarIssues(int carId, RequestCallback callback){
        get(String.format("car/%s/issues?type=upcoming", String.valueOf(carId)), callback);
    }

    public void getRandomVin(RequestCallback callback) {
        getWithCustomUrl("http://randomvin.com", "/getvin.php?type=valid", callback);
    }
}

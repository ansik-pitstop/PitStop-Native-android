package com.pitstop.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.Nullable;

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

    public void postNoAuth(String uri, RequestCallback callback, JSONObject body) { // for login, sign up, scans
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
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

    public void delete(String uri, RequestCallback callback) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + getAccessToken())
                .requestCallBack(callback)
                .requestType(RequestType.DELETE)
                .pitstopAuthApi(pitstopAuthApi)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    public void putNoAuth(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
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

    public void deleteUserCar(int userId, int carId, RequestCallback callback) {
//        /v1/users/3877/cars/96529
        delete("v1/users/" + userId + "/cars/" + carId, callback);
    }

    public void getShops(RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "getShops");
        get("shop", callback);
    }

    public void loginSocial(String accessToken, String provider, RequestCallback callback) {
        Log.i(TAG, "login");
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("accessToken", accessToken);
            credentials.put("provider", provider);
            credentials.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("login/social", callback, credentials);
    }

    public void loginAsync(String userName, String password, RequestCallback callback) {
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("username", userName);
            credentials.put("password", password);
            credentials.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("login", callback, credentials);
    }

    // for logged in parse user
    public void loginLegacy(String userId, String sessionToken, RequestCallback callback) {
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("userId", userId);
            credentials.put("sessionToken", sessionToken);
            credentials.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("login/legacy", callback, credentials);
    }

    public void signUpAsync(JSONObject newUser, RequestCallback callback) {
        postNoAuth("user", callback, newUser);
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

    public void sendTripStart(String scannerId, String rtcTime, String tripIdRaw, RequestCallback callback) {
        sendTripStart(scannerId, rtcTime, tripIdRaw, null, callback);
    }

    public void sendTripStart(String scannerId, String rtcTime, String tripIdRaw, @Nullable String mileage, RequestCallback callback) {

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

        postNoAuth("scan/trip", callback, body);
    }


    public void saveTripMileage(long tripId, String mileage, String rtcTime, RequestCallback callback) {

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

    public void getUser(int userId, RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "getUser: " + userId);
        get("user/" + userId, callback);
    }

    public void updateUser(int userId, String firstName, String lastName, String phoneNumber, RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, String.format("updateUser: %s, %s, %s, %s", userId, firstName, lastName, phoneNumber));

        try {
            JSONObject json = new JSONObject();
            json.put("userId", userId);
            json.put("firstName", firstName);
            json.put("lastName", lastName);
            json.put("phone", phoneNumber);
            put("user/", callback, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void resetPassword(String email, RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "resetPassword: " + email);

        try {
            postNoAuth("login/resetPassword", callback, new JSONObject().put("email", email));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void refreshToken(String refreshToken, Context context, RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "refreshToken: " + refreshToken);

        try {
            new HttpRequest.Builder().uri("login/refresh")
                    .header("Client-Id", SecretUtils.getClientId(context))
                    .body(new JSONObject().put("refreshToken", refreshToken))
                    .requestCallBack(callback)
                    .requestType(RequestType.POST)
                    .context(context)
                    .createRequest()
                    .executeAsync();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setNoMainCar(final int userId, final RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, String.format("setNoMainCar: userId: %s", userId));

        getUserSettingsById(userId, new RequestCallback() {
            // need to add option instead of replace
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null) {
                    try {
                        JSONObject options = new JSONObject(response).getJSONObject("user");
                        if (options.has("mainCar")){
                            options.remove("mainCar");
                        }

                        JSONObject putOptions = new JSONObject();
                        putOptions.put("settings",options);

                        put("user/" + userId + "/settings", callback, putOptions);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.done(response,requestError);
                    }
                }
                else{
                    callback.done(response,requestError);
                }
            }
        });
    }

    public void getLatestTrip(String scannerId, RequestCallback callback) {
        //Logger.getInstance().LOGI(TAG, "getLatestTrip: scannerId: " + scannerId);

        get(String.format("scan/trip/?scannerId=%s&latest=true&active=true", scannerId), callback);
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

    public void getAppointments(int carId, RequestCallback callback){
        get(String.format("car/%d/appointments",carId), callback);
    }

    public void getRandomVin(RequestCallback callback) {
        getWithCustomUrl("http://randomvin.com", "/getvin.php?type=valid", callback);
    }
    public void postTripStep1(Trip trip,String vin,RequestCallback callback){
        JSONObject body = new JSONObject();
        try {
            body.put("vin", vin);
            body.put("rtcTimeStart",trip.getStart().getTime()/1000L);//millisecond time to unix time
            body.put("deviceType","android");
            body.put("locationStart",new JSONObject()
                    .put("lat",Double.toString(trip.getStart().getLatitude()))
                    .put("long",Double.toString(trip.getStart().getLongitude())));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post("scan/trip",callback,body);
    }
}

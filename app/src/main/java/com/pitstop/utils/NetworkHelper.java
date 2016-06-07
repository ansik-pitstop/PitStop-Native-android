package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.castel.obd.info.PIDInfo;
import com.pitstop.BuildConfig;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.ServerAccess.HttpRequest;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestType;
import com.pitstop.application.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static final String devToken = "DINCPNWtqjjG69xfMWuF8BIJ8QjwjyLwCq36C19CkTIMkFnE6zSxz7Xoow0aeq8M6Tlkybu8gd4sDIKD";

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static void post(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("clientId", devToken) // TODO : put in tokens for prod
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .createRequest()
                .executeAsync();
    }

    private static void get(String uri, RequestCallback callback) {
        new HttpRequest.Builder().uri(uri)
                .header("clientId", devToken)
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .createRequest()
                .executeAsync();
    }

    private static void put(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("clientId", devToken)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.PUT)
                .createRequest()
                .executeAsync();
    }

    public static void createNewCar(int userId, int mileage, String vin, String scannerId,
                                    int shopId, RequestCallback callback) {
        Log.i(TAG, "createNewCar");
        JSONObject body = new JSONObject();

        try {
            body.put("vin", vin);
            body.put("baseMileage", mileage);
            body.put("userId", userId);
            body.put("scannerId", scannerId);
            body.put("shopId", shopId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("car", callback, body);
    }

    public static void getCarsByUserId(int userId, RequestCallback callback) {
        Log.i(TAG, "getCarsByUserId: " + userId);
        get("car/?userId=" + userId, callback);
    }

    public static void getCarsByVin(String vin, RequestCallback callback) {
        Log.i(TAG, "getCarsByVin: " + vin);
        get("car/?vin=" + vin, callback);
    }

    public static void getCarsById(int carId, RequestCallback callback) {
        Log.i(TAG, "getCarsById: " + carId);
        get("car/" + carId, callback);
    }

    public static void updateCarMileage(int carId, int mileage, RequestCallback callback) {
        Log.i(TAG, "updateCarShop: carId: " + carId + " mileage: " + mileage);
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("totalMileage", mileage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public static void updateCarShop(int carId, int shopId, RequestCallback callback) {
        Log.i(TAG, "updateCarShop: carId: " + carId + " shopId: " + shopId);
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("shopId", shopId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public static void getShops(RequestCallback callback) {
        Log.i(TAG, "getShops");
        get("shop", callback);
    }

    public static void updateFirstName(int userId, String firstName, String lastName, RequestCallback callback) {
        Log.i(TAG, "updateFirstName: userId: " + userId + " firstName: " + firstName + " lastName: " + lastName);
        JSONObject body = new JSONObject();

        try {
            body.put("userId", userId);
            body.put("firstName", firstName);
            body.put("lastName", lastName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("user", callback, body);
    }

    public static void loginAsync(String userName, String password, RequestCallback callback) {
        Log.i(TAG, "login");
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("username", userName);
            credentials.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("login", callback, credentials);
    }

    // for logged in parse user
    public static void loginLegacy(String userId, String sessionToken, RequestCallback callback) {
        Log.i(TAG, "login legacy");
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("userId", userId);
            credentials.put("sessionToken", sessionToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("login/legacy", callback, credentials);
    }

    public static void signUpAsync(JSONObject newUser, RequestCallback callback) {
        Log.i(TAG, "signup");
        post("user", callback, newUser);
    }

    public static void addNewDtc(int carId, int mileage, String rtcTime, String dtcCode, boolean isPending,
                                 List<PIDInfo> freezeData, RequestCallback callback) {
        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();

        try {
            for(PIDInfo info : freezeData) {
                data.put(new JSONObject().put("id", info.pidType).put("data", info.value));
            }

            body.put("carId", carId);
            body.put("issueType", CarIssue.DTC);
            body.put("data",
                    new JSONObject().put("mileage", mileage)
                            .put("rtcTime", Long.parseLong(rtcTime))
                            .put("dtcCode", dtcCode)
                            .put("isPending", isPending)
                            .put("freezeData", new JSONObject().put("data", data)));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("issue", callback, body);
    }

    public static void serviceDone(int carId, int issueId, int daysAgo, int mileage, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("issueId", issueId);
            body.put("daysAgo", daysAgo);
            body.put("mileage", mileage);
            body.put("status", CarIssue.ISSUE_DONE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("issue", callback, body);
    }

    public static void createNewScanner(int carId, String scannerId, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("scannerId", scannerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("scanner", callback, body);
    }

    public static void saveFreezeData(String scannerId, String serviceType, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("serviceType", serviceType);
            body.put("data", new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("scan/freezeData", callback, body);
    }

    public static void saveTripMileage(String scannerId, String tripId, String mileage, String rtcTime, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("tripId", Long.parseLong(tripId));
            body.put("mileage", Double.parseDouble(mileage));
            body.put("rtcTime", Long.parseLong(rtcTime));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("scan/tripMileage", callback, body);
    }

    public static void savePids(String scannerId, JSONArray pidArr, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("pidArray", pidArr); // TODO: fix format (should be object)
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("scan/pids", callback, body);
    }

    public static void requestService(int userId, int carId, int shopId, String comments,
                                      RequestCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("userId", userId);
            body.put("carId", carId);
            body.put("shopId", shopId);
            body.put("comments", comments);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("utility/serviceRequest", callback, body);
    }

    public static void requestService(int userId, int carId, int shopId, String comments,
                                      int issueId, RequestCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("userId", userId);
            body.put("carId", carId);
            body.put("shopId", shopId);
            body.put("comments", comments);
            body.put("issueId", issueId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("utility/serviceRequest", callback, body);
    }

    public static void getUser(int userId, RequestCallback callback) {
        Log.i(TAG, "getUser: " + userId);
        get("user/" + userId, callback);
    }
}

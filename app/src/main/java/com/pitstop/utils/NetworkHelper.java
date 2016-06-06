package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import com.castel.obd.info.PIDInfo;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.ServerAccess.HttpRequest;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

        new HttpRequest.Builder().uri("car")
                .requestType(RequestType.POST)
                .requestCallBack(callback)
                .body(body)
                .createRequest().executeAsync();
    }

    public static void getCarsByUserId(int userId, RequestCallback callback) {
        Log.i(TAG, "getCarsByUserId: " + userId);
        new HttpRequest.Builder().uri("car/?userId=" + userId)
                .requestType(RequestType.GET)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void getCarsByVin(String vin, RequestCallback callback) {
        Log.i(TAG, "getCarsByVin: " + vin);
        new HttpRequest.Builder().uri("car/?vin=" + vin)
                .requestType(RequestType.GET)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void getCarsById(int carId, RequestCallback callback) {
        Log.i(TAG, "getCarsById: " + carId);
        new HttpRequest.Builder().uri("car/" + carId)
                .requestType(RequestType.GET)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("car")
                .requestType(RequestType.PUT)
                .requestCallBack(callback)
                .body(body)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("car")
                .requestType(RequestType.PUT)
                .requestCallBack(callback)
                .body(body)
                .createRequest().executeAsync();
    }

    public static void getShops(RequestCallback callback) {
        Log.i(TAG, "getShops");
        new HttpRequest.Builder().uri("shop")
                .requestType(RequestType.GET)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("user")
                .requestType(RequestType.PUT)
                .requestCallBack(callback)
                .body(body)
                .createRequest().executeAsync();
    }

    public static void updateLastName(int userId, String newName, RequestCallback callback) {
        Log.i(TAG, "updateLastName: userId: " + userId + " newName: " + newName);
        JSONObject body = new JSONObject();

        try {
            body.put("userId", userId);
            body.put("lastName", newName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpRequest.Builder().uri("user")
                .requestType(RequestType.PUT)
                .requestCallBack(callback)
                .body(body)
                .createRequest().executeAsync();
    }

    public static void loginAsync(String userName, String password, RequestCallback callback) {

        JSONObject credentials = new JSONObject();
        try {
            credentials.put("username", userName);
            credentials.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpRequest.Builder().uri("login")
                .requestType(RequestType.POST)
                .body(credentials)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void signUpAsync(JSONObject newUser, RequestCallback callback) {
        new HttpRequest.Builder().uri("user")
                .requestType(RequestType.POST)
                .body(newUser)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void addNewDtc(int carId, int mileage, String rtcTime, String dtcCode, boolean isPending,
                                 List<PIDInfo> freezeData, RequestCallback callback) {
        JSONObject body = new JSONObject();
        // TODO: put actual freeze data

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

        new HttpRequest.Builder().uri("issue")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("issue")
                .requestType(RequestType.PUT)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void createNewScanner(int carId, String scannerId, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("scannerId", scannerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpRequest.Builder().uri("scanner")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("scan/freezeData")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("scan/tripMileage")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void savePids(String scannerId, JSONArray pidArr, RequestCallback callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("scannerId", scannerId);
            body.put("pidArray", pidArr); // TODO: fix format (should be object)
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpRequest.Builder().uri("scan/pids")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("utility/serviceRequest")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
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

        new HttpRequest.Builder().uri("utility/serviceRequest")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

    public static void getUser(int userId, RequestCallback callback) {
        Log.i(TAG, "getUser: " + userId);
        new HttpRequest.Builder().uri("user/" + userId)
                .requestType(RequestType.GET)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }
}

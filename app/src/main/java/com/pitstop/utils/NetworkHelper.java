package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pitstop.DataAccessLayer.ServerAccess.HttpRequest;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestType;

import org.json.JSONException;
import org.json.JSONObject;

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

    public static void createNewCar(int userId, int mileage, String vin, @Nullable String scannerId,
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

    public static void updateUserName(int userId, String newName, RequestCallback callback) {
        Log.i(TAG, "updateUserName: userId: " + userId + " newName: " + newName);
        JSONObject body = new JSONObject();

        try {
            body.put("userId", userId);
            body.put("firstName", newName);
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

    public static void addNewDtc(int carId, int mileage, int rtcTime, String dtcCode, boolean isPending, RequestCallback callback) {
        JSONObject body = new JSONObject();
        // TODO: put actual freeze data
        try {
            body.put("carId", carId);
            body.put("issueType", "dtc");
            body.put("data",
                    new JSONObject().put("mileage", mileage)
                            .put("rtcTime", rtcTime)
                            .put("dtcCode", dtcCode)
                            .put("isPending", isPending)
                            .put("freezeData", new JSONObject().put("data", "data")));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpRequest.Builder().uri("issue")
                .requestType(RequestType.POST)
                .body(body)
                .requestCallBack(callback)
                .createRequest().executeAsync();
    }

}

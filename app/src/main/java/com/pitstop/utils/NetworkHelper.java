package com.pitstop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.castel.obd.info.PIDInfo;
import com.parse.ParseInstallation;
import com.pitstop.BuildConfig;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.ServerAccess.HttpRequest;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestType;
import com.pitstop.application.GlobalApplication;

import static com.pitstop.utils.LogUtils.LOGI;
import static com.pitstop.utils.LogUtils.LOGV;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static final String clientId = BuildConfig.CLIENT_ID;

    private Context context;

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
                .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void post(String uri, RequestCallback callback, JSONObject body) {
        if(!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                .header("Authorization", "Bearer " + ((GlobalApplication) context).getAccessToken())
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void get(String uri, RequestCallback callback) {
        if(!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                .header("Authorization", "Bearer " + ((GlobalApplication) context).getAccessToken())
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void put(String uri, RequestCallback callback, JSONObject body) {
        if(!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                .header("Authorization", "Bearer " + ((GlobalApplication) context).getAccessToken())
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.PUT)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void putNoAuth(String uri, RequestCallback callback, JSONObject body) {
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.PUT)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    public void createNewCar(int userId, int mileage, String vin, String scannerId,
                                    int shopId, RequestCallback callback) {
        LOGI(TAG, "createNewCar");
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

    public void getCarsByUserId(int userId, RequestCallback callback) {
        LOGI(TAG, "getCarsByUserId: " + userId);
        get("car/?userId=" + userId, callback);
    }

    public void getCarsByVin(String vin, RequestCallback callback) {
        LOGI(TAG, "getCarsByVin: " + vin);
        get("car/?vin=" + vin, callback);
    }

    public void getCarsById(int carId, RequestCallback callback) {
        LOGI(TAG, "getCarsById: " + carId);
        get("car/" + carId, callback);
    }

    public void updateCarMileage(int carId, int mileage, RequestCallback callback) {
        LOGI(TAG, "updateCarShop: carId: " + carId + ", mileage: " + mileage);
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("totalMileage", mileage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public void updateCarShop(int carId, int shopId, RequestCallback callback) {
        LOGI(TAG, "updateCarShop: carId: " + carId + " shopId: " + shopId);
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("shopId", shopId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public void getShops(RequestCallback callback) {
        LOGI(TAG, "getShops");
        get("shop", callback);
    }

    public void updateFirstName(int userId, String firstName, String lastName, RequestCallback callback) {
        LOGI(TAG, "updateFirstName: userId: " + userId + " firstName: " + firstName + " lastName: " + lastName);
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

    public void loginAsync(String userName, String password, RequestCallback callback) {
        LOGI(TAG, "login");
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
        LOGI(TAG, "login legacy");
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
        LOGI(TAG, "signup");
        postNoAuth("user", callback, newUser);
    }

    public void addNewDtc(int carId, double mileage, String rtcTime, String dtcCode, boolean isPending,
                                 List<PIDInfo> freezeData, RequestCallback callback) {
        LOGI(TAG, String.format("addNewDtc: carId: %s, mileage: %s," +
                " rtcTime: %s, dtcCode: %s, isPending: %s", carId, mileage, rtcTime, dtcCode, isPending));

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
                            .put("isPending", isPending));
                            //.put("freezeData", data));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("issue", callback, body);
    }

    public void serviceDone(int carId, int issueId, int daysAgo, double mileage, RequestCallback callback) {
        LOGI(TAG, String.format("serviceDone: carId: %s, issueId: %s," +
                " daysAgo: %s, mileage: %s", carId, issueId, daysAgo, mileage));

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

    public void servicePending(int carId, int issueId, RequestCallback callback) {
        LOGI(TAG, String.format("servicePending: carId: %s, issueId: %s,", carId, issueId));

        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("issueId", issueId);
            body.put("status", CarIssue.ISSUE_PENDING);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("issue", callback, body);
    }

    public void createNewScanner(int carId, String scannerId, RequestCallback callback) {
        LOGI(TAG, String.format("createNewScanner: carId: %s, scannerId: %s,", carId, scannerId));

        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("scannerId", scannerId);
            body.put("isActive", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("scanner", callback, body);
    }

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
            tripBody.put("mileage", Double.parseDouble(mileage)/1000);
            tripBody.put("tripId", tripId);
            tripBody.put("rtcTimeEnd", Long.parseLong(rtcTime));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        putNoAuth("scan/trip", callback, tripBody);
    }

    public void savePids(int tripId, String scannerId, JSONArray pidArr, RequestCallback callback) {
        LOGI(TAG, "savePids to " + scannerId);
        LOGV(TAG, "pidArr: "  + pidArr.toString());

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

    public void requestService(int userId, int carId, int shopId, String comments,
                                      RequestCallback callback) {
        LOGI(TAG, String.format("requestService: userId: %s, carId: %s, shopId: %s", userId, carId, shopId));

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

    public void requestService(int userId, int carId, int shopId, String comments,
                                      int issueId, RequestCallback callback) {
        LOGI(TAG, String.format("requestService: userId: %s, carId: %s, shopId: %s", userId, carId, shopId));

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

    public void getUser(int userId, RequestCallback callback) {
        LOGI(TAG, "getUser: " + userId);
        get("user/" + userId, callback);
    }

    public void resetPassword(String email, RequestCallback callback) {
        LOGI(TAG, "resetPassword: " + email);

        try {
            postNoAuth("login/resetPassword", callback, new JSONObject().put("email", email));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void refreshToken(String refreshToken, RequestCallback callback) {
        LOGI(TAG, "refreshToken: " + refreshToken);

        try {
            new HttpRequest.Builder().uri("login/refresh")
                    .header("Client-Id", BuildConfig.DEBUG ? clientId : clientId)
                    .body(new JSONObject().put("refreshToken", refreshToken))
                    .requestCallBack(callback)
                    .requestType(RequestType.POST)
                    .context(null)
                    .createRequest()
                    .executeAsync();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setMainCar(int userId, int carId, RequestCallback callback) {
        LOGI(TAG, String.format("setMainCar: userId: %s, carId: %s", userId, carId));

        JSONObject body = new JSONObject();

        try {
            body.put("settings", new JSONObject().put("mainCar", carId));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("user/" + userId + "/settings", callback, body);
    }

    public void getLatestTrip(String scannerId, RequestCallback callback) {
        LOGI(TAG, "getLatestTrip: scannerId: " + scannerId);

        get(String.format("scan/trip/?scannerId=%s&latest=true&active=true", scannerId), callback);
    }
}

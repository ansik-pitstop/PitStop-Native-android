package com.pitstop.utils;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseInstallation;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.models.CarIssue;
import com.pitstop.models.Trip;
import com.pitstop.models.TripLocation;
import com.pitstop.network.HttpRequest;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.network.RequestType;
import com.pitstop.ui.service_request.ServiceRequestActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.pitstop.utils.LogUtils.LOGI;
import static com.pitstop.utils.LogUtils.LOGV;

/**
 * Created by Ben Wu on 2016-05-20.
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private final String CLIENT_ID;

    private static final String INSTALLATION_ID_KEY = "installationId";

    private Context context;

    public NetworkHelper(Context context) {
        this.context = context;
        CLIENT_ID = SecretUtils.getClientId(context);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void getWithCustomUrl(String url, String uri, RequestCallback callback) {
        new HttpRequest.Builder()
                .url(url)
                .uri(uri)
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void postNoAuth(String uri, RequestCallback callback, JSONObject body) { // for login, sign up, scans
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void post(String uri, RequestCallback callback, JSONObject body) {
        if (!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + ((GlobalApplication) context).getAccessToken())
                .body(body)
                .requestCallBack(callback)
                .requestType(RequestType.POST)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void get(String uri, RequestCallback callback) {
        if (!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + ((GlobalApplication) context).getAccessToken())
                .requestCallBack(callback)
                .requestType(RequestType.GET)
                .context(context)
                .createRequest()
                .executeAsync();
    }

    private void put(String uri, RequestCallback callback, JSONObject body) {
        if (!isConnected(context)) {
            Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        new HttpRequest.Builder().uri(uri)
                .header("Client-Id", CLIENT_ID)
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
                .header("Client-Id", CLIENT_ID)
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

    public void createNewCarWithoutShopId(int userId, int mileage, String vin, String scannerId,
                                          RequestCallback callback){
        LOGI(TAG, "createNewCarWithoutShopId");
        JSONObject body = new JSONObject();

        try {
            body.put("vin", vin);
            body.put("baseMileage", mileage);
            body.put("userId", userId);
            body.put("scannerId", scannerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("car", callback, body);
    }

    public void deleteUserCar(int carId, RequestCallback callback) {
        LOGI(TAG, "Delete car: " + carId);

        JSONObject body = new JSONObject();
        try {
            body.put("userId", 0);
            body.put("carId", carId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
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

    public void updateCar(int carId, double mileage, int shopId, RequestCallback callback){
        LOGI(TAG, "updateCar: carId: " + carId + ", mileage: " + mileage +"shopId: "+shopId);
        JSONObject body = new JSONObject();

        try {
            body.put("carId", carId);
            body.put("totalMileage", mileage);
            body.put("shopId", shopId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("car", callback, body);
    }

    public void updateCarMileage(int carId, double mileage, RequestCallback callback) {
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

    /**
     * Allow the user to change his/her phone number in the preference
     *
     * @param userId
     * @param phoneNumber
     * @param callback
     */
    public void updateUserPhone(int userId, String phoneNumber, RequestCallback callback) {
        LOGI(TAG, "updatePhoneNumber: userId: " + userId + " phoneNUmber: " + phoneNumber);
        JSONObject body = new JSONObject();

        try {
            body.put("userId", userId);
            body.put("phone", phoneNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        put("user", callback, body);
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

    public void addNewDtc(int carId, double mileage, String rtcTime, String dtcCode, boolean isPending, RequestCallback callback) {
        LOGI(TAG, String.format("addNewDtc: carId: %s, mileage: %s," +
                " rtcTime: %s, dtcCode: %s, isPending: %s", carId, mileage, rtcTime, dtcCode, isPending));

        JSONObject body = new JSONObject();
        //JSONArray data = new JSONArray(); // TODO: Freeze data

        try {
            //for(PIDInfo info : freezeData) {
            //    data.put(new JSONObject().put("id", info.pidType).put("data", info.value));
            //}

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
     * Endpoint = POST /car/{carId}/service
     *
     * @param carId
     * @param issueId
     * @param callback
     */
    public void postPresetIssue(int carId, int issueId, RequestCallback callback) {
        LOGI(TAG, String.format("postPresetIssue: carId: %s, issueId: %s, " +
                "issueType: %s", carId, issueId, CarIssue.TYPE_PRESET));
        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();

        try {
            data.put(new JSONObject()
                    .put("type", CarIssue.TYPE_PRESET)
                    .put("id", issueId));
            body.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post("car/" + carId + "/service", callback, body);
    }

    /**
     * Endpoint = POST /car/{carId}/service
     *
     * @param carId       car id that issue needs to be added to
     * @param item        the issue item
     * @param action      the issue action
     * @param description the issue description
     * @param priority    the issue priority
     * @param callback
     */
    public void postUserInputIssue(int carId, String item, String action,
                                   String description, int priority, RequestCallback callback) {
        LOGI(TAG, String.format("postPresetIssue: carId: %s, item: %s, " +
                "issueType: %s", carId, item, CarIssue.TYPE_USER_INPUT));

        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();

        try {
            data.put(new JSONObject()
                    .put("type", CarIssue.TYPE_USER_INPUT)
                    .put("item", item)
                    .put("action", action)
                    .put("description", description)
                    .put("priority", priority));
            body.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("car/" + carId + "/service", callback, body);
    }

    /**
     * Used to post multiple preset issues at a time.
     *
     * @param carId
     * @param pickedIssues
     * @param callback
     */
    public void postMultiplePresetIssue(int carId, List<CarIssue> pickedIssues, RequestCallback callback) {
        LOGI(TAG, "Post multiple preset issues: carId: " + carId + ", pickedIssues: " + pickedIssues.size());

        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();
        try {
            for (CarIssue issue : pickedIssues) {
                if (issue.getIssueType().equals(CarIssue.TYPE_PRESET)) {
                    data.put(new JSONObject()
                            .put("type", issue.getIssueType())
                            .put("status", issue.getStatus())
                            .put("id", issue.getId()));
                } else {
                    data.put(new JSONObject()
                            .put("type", issue.getIssueType())
                            .put("item", issue.getItem())
                            .put("action", issue.getAction())
                            .put("description", issue.getDescription())
                            .put("priority", issue.getPriority()));
                }
            }
            body.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post("car/" + carId + "/service", callback, body);
    }

    /**
     * Get an array of categories, each category contains an array of services
     *
     * @param carId
     * @param callback
     */
    public void getPresetIssuesByCarId(int carId, RequestCallback callback) {
        LOGI(TAG, String.format("getPresetIssuesByCarId: carId: %s", carId));
        get("car/" + carId + "/service", callback);
    }

    public void setIssueDone(int carId, int issueId, int daysAgo, double mileage, RequestCallback callback) {
        LOGI(TAG, String.format("setIssueDone: carId: %s, issueId: %s," +
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

    public void setIssuePending(int carId, int issueId, RequestCallback callback) {
        LOGI(TAG, String.format("setIssuePending: carId: %s, issueId: %s,", carId, issueId));

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
            body.put("data", new JSONObject()); //?
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postNoAuth("scan/freezeData", callback, body);
    }



    public void sendTripStart(String scannerId, String rtcTime, String tripIdRaw, RequestCallback callback) {
        sendTripStart(scannerId, rtcTime, tripIdRaw, null, callback);
    }

    public void sendTripStart(String scannerId, String rtcTime, String tripIdRaw, @Nullable String mileage, RequestCallback callback) {
        LOGI(TAG, String.format("sendTripStart: scannerId: %s, rtcTime: %s", scannerId, rtcTime));

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
     * Called when sending request for service via "send request" button, etc.
     *
     * @param userId
     * @param carId
     * @param shopId
     * @param state                for now valid values are ["tentative","requested"]
     * @param appointmentTimestamp ISO8601 format, e.g. 2016-08-31T20:43:25+00:00
     * @param callback
     */
    public void requestService(int userId, int carId, int shopId, String state, String appointmentTimestamp,
                               String comments, RequestCallback callback) {
        LOGI(TAG, String.format("requestService: userId: %s, carId: %s, shopId: %s", userId, carId, shopId));

        JSONObject body = new JSONObject();
        JSONObject options = new JSONObject();
        try {
            body.put("userId", userId);
            body.put("carId", carId);
            body.put("shopId", shopId);
            body.put("comments", comments);
            options.put("state", state);
            options.put("appointmentDate", appointmentTimestamp);
            body.put("options", options);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("utility/serviceRequest", callback, body);

        // If state is tentative, we put salesPerson to another endpoint
        if (state.equals(ServiceRequestActivity.STATE_TENTATIVE)) {
            JSONObject updateSalesman = new JSONObject();
            try {
                updateSalesman.put("carId", carId);
                updateSalesman.put("salesperson", comments);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            put("car", new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError != null) {
                        Log.e(TAG, "Status code: " + requestError.getStatusCode()
                                + "Error: " + requestError.getError()
                                + "Message " + requestError.getMessage());
                    } else {
                        Log.e(TAG, "All good");
                    }
                }
            }, updateSalesman);
        }

    }

    public void getUser(int userId, RequestCallback callback) {
        LOGI(TAG, "getUser: " + userId);
        get("user/" + userId, callback);
    }

    public void updateUser(int userId, String firstName, String lastName, String phoneNumber, RequestCallback callback) {
        LOGI(TAG, String.format("updateUser: %s, %s, %s, %s", userId, firstName, lastName, phoneNumber));

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
        LOGI(TAG, "resetPassword: " + email);

        try {
            postNoAuth("login/resetPassword", callback, new JSONObject().put("email", email));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void refreshToken(String refreshToken, Context context, RequestCallback callback) {
        LOGI(TAG, "refreshToken: " + refreshToken);

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

    public void getMainCar(final int userId, final RequestCallback callback){
        getUserSettingsById(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject options = new JSONObject(response);
                        int mainCarId = options.getJSONObject("user").getInt("mainCar");
                        getCarsById(mainCarId,callback);
                    }
                    catch(JSONException e){
                        Log.d("TAG","JSONException Caught!");
                    }
                }
                else{
                    callback.done(response,requestError);
                }


            }
        });
    }

    public void setMainCar(final int userId, final int carId, final RequestCallback callback) {
        LOGI(TAG, String.format("setMainCar: userId: %s, carId: %s", userId, carId));

        getUser(userId, new RequestCallback() {
            // need to add option instead of replace
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null) {
                    try {
                        JSONObject options = new JSONObject(response).getJSONObject("settings");
                        options.put("settings", new JSONObject().put("mainCar", carId));
                        put("user/" + userId + "/settings", callback, options);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    callback.done(response,requestError);
                }
            }
        });
    }

    public void getLatestTrip(String scannerId, RequestCallback callback) {
        LOGI(TAG, "getLatestTrip: scannerId: " + scannerId);

        get(String.format("scan/trip/?scannerId=%s&latest=true&active=true", scannerId), callback);
    }

    public void updateMileageStart(double mileageStart, int tripId, RequestCallback callback) {
        LOGI(TAG, "updateMileageStart: mileage: " + mileageStart + ", tripId: " + tripId);

        JSONObject body = new JSONObject();

        try {
            body.put("mileageStart", mileageStart);
            body.put("tripId", tripId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        putNoAuth("scan/trip", callback, body);
    }

    /**
     * Get the aggregated settings
     *
     * @param userId
     * @param callback
     */
    public void getUserSettingsById(int userId, RequestCallback callback) {
        //GET /settings?userId=
        LOGI(TAG, "getUserSettingsById: " + userId);
        get("settings/?userId=" + userId, callback);
    }

    /**
     * Append/Replace field "isTutorialDone" with value true
     * example value:
     * {
     * "settings": {}
     * }
     *
     * @param userId                 current user id
     * @param aggregatedUserSettings extracted "user" object from the aggregated settings
     * @param callback
     */
    public void setTutorialDone(int userId, JSONObject aggregatedUserSettings, RequestCallback callback) {
        LOGI(TAG, "setTutorialDone: " + userId);

        JSONObject body = aggregatedUserSettings;
        JSONObject settings = new JSONObject();

        try {
            body.put("isTutorialDone", true);
            settings.put("settings", body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //PUT /user/{userId}/settings
        put("user/" + userId + "/settings", callback, settings);
    }

    public void setTutorialUndone(int userId, JSONObject initialUserSettings, RequestCallback callback) {
        LOGI(TAG, "setTutorialUndone: " + userId);
        JSONObject settings = new JSONObject();

        try {
            initialUserSettings.put("isTutorialDone", false);
            settings.put("settings", initialUserSettings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //PUT /user/{userId}/settings
        put("user/" + userId + "/settings", callback, settings);
    }

    public void validateScannerId(String scannerId, RequestCallback callback) {
        LOGI(TAG, "validate scanner id: " + scannerId);
        get("scanner/?scannerId=" + scannerId + "&active=true", callback);
    }

    public void getCurrentCarIssues(int carId, RequestCallback callback){
        get(String.format("car/%s/issues?type=active", String.valueOf(carId)), callback);
    }

    public void getDoneCarIssues(int carId, RequestCallback callback){
        get(String.format("car/%s/issues?type=history", String.valueOf(carId)), callback);
    }

    public void getUpcomingCarIssues(int carId, RequestCallback callback){
        get(String.format("car/%s/issues?type=upcoming", String.valueOf(carId)), callback);
    }

    public void getUserInstallationId(int userId, final RequestCallback callback){
        getUser(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                JSONObject jObject  = null;
                String installationIDResponse = "";
                try {
                    jObject = new JSONObject(response);
                    JSONArray data = jObject.getJSONArray(INSTALLATION_ID_KEY);
                    installationIDResponse = data.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                callback.done(installationIDResponse, requestError);
            }
        });

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

    public void postTripStep2(Trip trip,RequestCallback callback){
        JSONObject body = new JSONObject();
        JSONArray pidArray = new JSONArray();
        try {
            body.put("tripId",trip.getId());
            for(TripLocation loc : trip.getPath()){
                pidArray.put(new JSONObject()
                .put("pids",new JSONArray())
                .put("geolocation",new JSONObject().put("lat",Double.toString(loc.getLatitude())).put("long",Double.toString(loc.getLongitude())))
                .put("rtcTime",loc.getTime()/1000L));
            }
            body.put("pidArray",pidArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post("scan/pids",callback,body);
    }

    public void putTripStep3(Trip trip, RequestCallback callback){//still need to add put statement
        JSONObject body = new JSONObject();
        try {
            body.put("tripId",trip.getId());
            body.put("rtcTimeEnd",trip.getEnd().getTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //put("scan/trip",callback,body);
       /// System.out.println("Testing "+body);
    }


}

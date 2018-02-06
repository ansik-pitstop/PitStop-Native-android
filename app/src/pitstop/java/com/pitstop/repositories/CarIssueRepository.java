package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.models.Appointment;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Timeline;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * CarIssue repository, use this class to modify, retrieve, and delete car issue data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

public class CarIssueRepository implements Repository{
    private final String TAG = getClass().getSimpleName();
    private final String END_POINT_REQUEST_SERVICE = "utility/serviceRequest";
    private final String END_POINT_ISSUES_UPCOMING = "car/%s/issues?type=upcoming";
    private final String END_POINT_ISSUES_CURRENT = "car/%s/issues?type=active";

    private static final int DEALERSHIP_ISSUES = 0;

    private static CarIssueRepository INSTANCE;
    private LocalCarIssueStorage carIssueAdapter; //To be integrated once refactored
    private NetworkHelper networkHelper;

    public static synchronized CarIssueRepository getInstance(LocalCarIssueStorage localCarIssueStorage
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new CarIssueRepository(localCarIssueStorage,networkHelper);
        }
        return INSTANCE;
    }

    public CarIssueRepository(LocalCarIssueStorage localCarIssueStorage, NetworkHelper networkHelper){
        this.carIssueAdapter = localCarIssueStorage;
        this.networkHelper = networkHelper;
    }

    public void insertDtc(int carId, double mileage,long rtcTime, String dtcCode, boolean isPending
            , Callback<String> callback){
        Log.d(TAG,"insertDtc() dtcCode: "+dtcCode);
        JSONObject body = new JSONObject();

        try {

            body.put("carId", carId);
            body.put("issueType", CarIssue.DTC);
            body.put("data",
                    new JSONObject().put("mileage", mileage)
                            .put("rtcTime", rtcTime)
                            .put("dtcCode", dtcCode)
                            .put("isPending", isPending));
            //.put("freezeData", data));
        } catch (JSONException e) {
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            e.printStackTrace();
        }
        networkHelper.post("issue", (response, requestError) -> {
            if (requestError == null){
                callback.onSuccess(dtcCode);
            }else{
                callback.onError(requestError);
                Log.d(TAG,"insertDtc() ERROR: "
                        +requestError.getMessage()+", body: "+body.toString());
            }
        }, body);
    }

    public void insert(CarIssue issue, Callback<Object> callback) {

        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();

        try{
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
        catch(JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            e.printStackTrace();
        }
        Log.d(TAG,"insert() body: "+body.toString());
        networkHelper.post("car/" + issue.getCarId() + "/service"
                , getInsertCarIssueRequestCallback(callback), body);
    }

    public void insert(int carId,List<CarIssue> issues, Callback<Object> callback){
        JSONObject body = new JSONObject();
        JSONArray data = new JSONArray();
        try {
            for (CarIssue issue : issues) {
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
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            e.printStackTrace();
        }

        Log.d(TAG,"insert() body: "+body.toString());
        networkHelper.post("car/" + carId + "/service"
                , getInsertCarIssuesRequestCallback(callback), body);
    }

    private RequestCallback getInsertCarIssuesRequestCallback(Callback<Object> callback){
        RequestCallback requestCallback = (response, requestError) -> {
            if(requestError == null){
                callback.onSuccess(response);

            }else{
                callback.onError(requestError);
            }
        };
        return requestCallback;
    }

    private RequestCallback getInsertCarIssueRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            try {
                if (requestError == null){
                    callback.onSuccess(response);
                }
                else{
                    callback.onError(requestError);
                }
            }
            catch(JsonIOException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                callback.onError(RequestError.getUnknownError());
            }
        };

        return requestCallback;
    }

    public void insertCustom(int carId, int userid, CarIssue issue,Callback<CarIssue> callback){
        JSONObject body = new JSONObject();
        try{
            body.put("carId",carId);
            body.put("issueType","customUser");
            JSONObject data = new JSONObject();
            data.put("item",issue.getItem());
            data.put("action",issue.getAction());
            data.put("priority",issue.getPriority());
            data.put("description",issue.getDescription());
            data.put("userId",userid);
            body.put("data",data);

        }catch (JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            callback.onError(RequestError.getUnknownError());
        }
        networkHelper.post("issue",getInsertCustomRequestCallback(callback,carId),body);

    }
    public RequestCallback getInsertCustomRequestCallback(Callback<CarIssue> callback,int carId){
        RequestCallback requestCallback = (response, requestError) -> {
            if(requestError == null){
                CarIssue issue = new CarIssue();
                try{
                    JSONObject responseJson = new JSONObject(response);
                    issue.setCarId(carId);
                    issue.setId(responseJson.getInt("id"));
                    issue.setItem(responseJson.getString("item"));
                    issue.setAction(responseJson.getString("action"));
                    issue.setDescription(responseJson.getString("description"));
                    issue.setIssueType(CarIssue.SERVICE_USER);
                    callback.onSuccess(issue);
                }catch (JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onSuccess(new CarIssue());
                }
            }else{
              callback.onError(requestError);
            }
        };
        return requestCallback;
    }

    public void updateCarIssue(CarIssue issue, Callback<CarIssue> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("carId", issue.getCarId());
            body.put("issueId", issue.getId());
            body.put("status", issue.getStatus());

            if (issue.getStatus().equals(CarIssue.ISSUE_DONE)){
                body.put("daysAgo",issue.getDaysAgo());
                body.put("mileage", issue.getDoneMileage());
            }

        } catch (JSONException e) {
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            e.printStackTrace();
        }

        networkHelper.put("issue", getUpdateCarIssueRequestCallback(callback), body);

    }

    private RequestCallback getUpdateCarIssueRequestCallback(Callback<CarIssue> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            try {
                if (requestError == null){
                    try{
                        CarIssue carIssue = CarIssue.createCarIssue(new JSONObject(response), 0);
                        callback.onSuccess(carIssue);
                    }catch(JSONException e){
                        Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                        callback.onError(RequestError.getUnknownError());
                    }
                }
                else{
                    callback.onError(requestError);
                }
            }
            catch(JsonIOException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                callback.onError(RequestError.getUnknownError());
            }
        };

        return requestCallback;
    }

    public void getUpcomingCarIssues(int carId, Callback<List<UpcomingIssue>> callback){
        networkHelper.get(String.format(END_POINT_ISSUES_UPCOMING, String.valueOf(carId))
                , getUpcomingCarIssuesRequestCallback(callback));

    }

    private RequestCallback getUpcomingCarIssuesRequestCallback(Callback<List<UpcomingIssue>> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    Timeline timelineData = new Gson().fromJson(response, Timeline.class);
                    List<UpcomingIssue> issues = timelineData.getResults().get(DEALERSHIP_ISSUES)
                            .getUpcomingIssues();
                    callback.onSuccess(issues);
                }
                else{
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public  void getCurrentCarIssues(int carId, Callback<List<CarIssue>> callback){
        networkHelper.get(String.format(END_POINT_ISSUES_CURRENT, String.valueOf(carId))
                , getCurrentCarIssuesRequestCallback(carId,callback));
    }

    private RequestCallback getCurrentCarIssuesRequestCallback(final int carId, Callback<List<CarIssue>> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            try {
                if (requestError == null){
                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray jsonArray = jsonObject.getJSONArray("results");

                    ArrayList<CarIssue> carIssues = CarIssue.createCarIssues(jsonArray,carId);
                    callback.onSuccess(carIssues);
                }
                else{
                    callback.onError(requestError);
                }
            }
            catch(JSONException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                callback.onError(RequestError.getUnknownError());
            }
        };

        return requestCallback;
    }


    public void requestService(int userId, int carId, Appointment appointment
            , Callback<Object> callback ){

        JSONObject body = new JSONObject();
        JSONObject options = new JSONObject();
        try {
            body.put("userId", userId);
            body.put("carId", carId);
            body.put("shopId", appointment.getShopId());
            body.put("comments", appointment.getComments());
            options.put("state", appointment.getState());
            String stringDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA)
                    .format(appointment.getDate());
            options.put("appointmentDate", stringDate);
            body.put("options", options);
        } catch (JSONException e) {
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            e.printStackTrace();
            callback.onError(RequestError.getUnknownError());
        }
        networkHelper.post(END_POINT_REQUEST_SERVICE, getRequestServiceCallback(callback), body);

        // If state is tentative, we put salesPerson to another endpoint
        if (appointment.getState().equals(RequestServiceActivity.STATE_TENTATIVE)) {
            JSONObject updateSalesman = new JSONObject();
            try {
                updateSalesman.put("carId", carId);
                updateSalesman.put("salesperson", appointment.getComments());
            } catch (JSONException e) {
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                e.printStackTrace();
            }
            networkHelper.put("car", (response, requestError) -> {
            }, updateSalesman);
        }
    }

    private RequestCallback getRequestServiceCallback(Callback<Object> callback){
        return (response, requestError) -> {
            if (requestError == null){
                callback.onSuccess(response);
                return;
            }
            else{
                callback.onError(requestError);
            }
        };
    }

    public void getDoneCarIssues(int carId, Callback<List<CarIssue>> callback){
        networkHelper.get(String.format("car/%s/issues?type=history", String.valueOf(carId))
                , getDoneCarIssuesRequestCallback(carId,callback));
        //return carIssueAdapter.getAllDoneCarIssues();
    }

    private RequestCallback getDoneCarIssuesRequestCallback(final int carId, Callback<List<CarIssue>> callback){
        //Create corresponding request callback

        return (response, requestError) -> {
            try {
                if (requestError == null){
                    ArrayList<CarIssue> carIssues = new ArrayList<>();
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                    carIssues = CarIssue.createCarIssues(jsonArray,carId);

                    callback.onSuccess(carIssues);
                }
                else{
                    callback.onError(requestError);
                }
            }
            catch(JSONException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                callback.onError(RequestError.getUnknownError());
            }
        };
    }
}

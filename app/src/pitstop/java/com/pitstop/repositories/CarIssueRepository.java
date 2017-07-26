package com.pitstop.repositories;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.Appointment;
import com.pitstop.models.Timeline;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//MAY BE USELESS FOR NOW SINCE CAR ISSUES ARE TIED TO CAR REPOSITORY ANYWAY, NOT IMPLEMENTED FULLY

/**
 * CarIssue repository, use this class to modify, retrieve, and delete car issue data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

public class CarIssueRepository implements Repository{

    private final String END_POINT_REQUEST_SERVICE = "utility/serviceRequest";
    private final String END_POINT_ISSUES_UPCOMING = "car/%s/issues?type=upcoming";
    private final String END_POINT_ISSUES_CURRENT = "car/%s/issues?type=active";

    public static final int DEALERSHIP_ISSUES = 0;

    private static CarIssueRepository INSTANCE;
    private LocalCarIssueAdapter carIssueAdapter;
    private NetworkHelper networkHelper;

    public static synchronized CarIssueRepository getInstance(LocalCarIssueAdapter localCarIssueAdapter
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new CarIssueRepository(localCarIssueAdapter,networkHelper);
        }
        return INSTANCE;
    }

    public CarIssueRepository(LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper){
        this.carIssueAdapter = localCarIssueAdapter;
        this.networkHelper = networkHelper;
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
            e.printStackTrace();
        }

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
            e.printStackTrace();
        }

        networkHelper.post("car/" + carId + "/service"
                , getInsertCarIssuesRequestCallback(callback), body);
    }

    private RequestCallback getInsertCarIssuesRequestCallback(Callback<Object> callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null && response != null){
                    callback.onSuccess(response);

                }else{
                    callback.onError(requestError);
                }
            }
        };
        return requestCallback;
    }

    private RequestCallback getInsertCarIssueRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(response);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };

        return requestCallback;
    }

    public void updateCarIssue(CarIssue issue, Callback<Object> callback) {
        JSONObject body = new JSONObject();

        try {
            body.put("carId", issue.getCarId());
            body.put("issueId", issue.getId());
            body.put("status", issue.getStatus());

            if (issue.getStatus().equals(CarIssue.ISSUE_DONE)){
                body.put("daysAgo", issue.getDaysAgo());
                body.put("mileage", issue.getDoneMileage());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        networkHelper.put("issue", getUpdateCarIssueRequestCallback(callback), body);

    }

    private RequestCallback getUpdateCarIssueRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(response);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    callback.onError(RequestError.getUnknownError());
                }
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
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
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
                    callback.onError(RequestError.getUnknownError());
                }
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
            options.put("appointmentDate", appointment.getDate());
            body.put("options", options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        networkHelper.post(END_POINT_REQUEST_SERVICE, getRequestServiceCallback(callback), body);

        // If state is tentative, we put salesPerson to another endpoint
        if (appointment.getState().equals(RequestServiceActivity.STATE_TENTATIVE)) {
            JSONObject updateSalesman = new JSONObject();
            try {
                updateSalesman.put("carId", carId);
                updateSalesman.put("salesperson", appointment.getComments());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            networkHelper.put("car", new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                }
            }, updateSalesman);
        }
    }

    private RequestCallback getRequestServiceCallback(Callback<Object> callback){
        return new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    callback.onSuccess(response);
                    return;
                }
                else{
                    callback.onError(requestError);
                }
            }
        };
    }

    public void getDoneCarIssues(int carId, Callback<List<CarIssue>> callback){
        networkHelper.getDoneCarIssues(carId,getDoneCarIssuesRequestCallback(carId,callback));
        //return carIssueAdapter.getAllDoneCarIssues();
    }

    private RequestCallback getDoneCarIssuesRequestCallback(final int carId, Callback<List<CarIssue>> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
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
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };

        return requestCallback;
    }
}

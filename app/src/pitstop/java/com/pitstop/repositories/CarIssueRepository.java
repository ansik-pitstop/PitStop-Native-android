package com.pitstop.repositories;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.Timeline;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
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

    public boolean insert(CarIssue model, Callback<Object> callback) {
        carIssueAdapter.storeCarIssue(model);
        networkHelper.postUserInputIssue(model.getCarId(),model.getItem(),model.getAction()
                ,model.getDescription(),model.getPriority(),getInsertCarIssueRequestCallback(callback));
        return true;
    }

    public void insert(int carId,List<CarIssue> issues, Callback<Object> callback){
        carIssueAdapter.storeIssues(issues);
        networkHelper.postMultiplePresetIssue(carId, issues, getgetInsertCarIssuesRequestCallback(callback));
    }

    private RequestCallback getgetInsertCarIssuesRequestCallback(Callback<Object> callback){
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

    public boolean updateCarIssue(CarIssue model, Callback<Object> callback) {
        carIssueAdapter.updateCarIssue(model);

        if (model.getStatus().equals(CarIssue.ISSUE_DONE)){
            networkHelper.setIssueDone(model.getCarId(),model.getId(),model.getDaysAgo()
                    ,model.getDoneMileage(),getUpdateCarIssueRequestCallback(callback));
        }
        else if (model.getStatus().equals(CarIssue.ISSUE_PENDING)){
            networkHelper.setIssuePending(model.getCarId(),model.getId()
                    ,getUpdateCarIssueRequestCallback(callback));
        }

        return false;
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

    public synchronized void getUpcomingCarIssues(int carId, Callback<List<UpcomingIssue>> callback){
        networkHelper.getUpcomingCarIssues(carId,getUpcomingCarIssuesRequestCallback(callback));
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
        networkHelper.getCurrentCarIssues(carId,getCurrentCarIssuesRequestCallback(carId,callback));
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


    public void requestService(int userId,int carId, int shopId,String state, String appointmentTimeStamp, String comments, RequestCallback callback ){
        networkHelper.requestService(userId,carId,shopId,state ,appointmentTimeStamp, comments,callback);
    }

    public  List<CarIssue> getDoneCarIssues(int carId, Callback<List<CarIssue>> callback){
        networkHelper.getDoneCarIssues(carId,getDoneCarIssuesRequestCallback(carId,callback));
        return carIssueAdapter.getAllDoneCarIssues();
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

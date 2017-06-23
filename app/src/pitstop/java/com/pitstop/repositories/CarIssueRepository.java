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

public class CarIssueRepository {

    private final int MAX_RESPONSE_LENGTH = 70000;
    public static final int DEALERSHIP_ISSUES = 0;

    private static CarIssueRepository INSTANCE;
    private LocalCarIssueAdapter carIssueAdapter;
    private NetworkHelper networkHelper;

    public interface CarIssueInsertCallback{
        void onCarIssueAdded();
        void onError();
    }

    public interface CarIssueUpdateCallback{
        void onCarIssueUpdated();
        void onError();
    }

    public interface CarIssueGetUpcomingCallback{
        void onCarIssueGotUpcoming(List<UpcomingIssue> carIssueUpcoming);
        void onError();
    }

    public interface CarIssueGetCurrentCallback{
        void onCarIssueGotCurrent(List<CarIssue> carIssueCurrent);
        void onError();
    }

    public interface CarIssueGetDoneCallback{
        void onCarIssueGotDone(List<CarIssue> carIssueDone);
        void onError();
    }

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

    public synchronized boolean insert(CarIssue model, CarIssueInsertCallback callback) {
        carIssueAdapter.storeCarIssue(model);
        networkHelper.postUserInputIssue(model.getCarId(),model.getItem(),model.getAction()
                ,model.getDescription(),model.getPriority(),getInsertCarIssueRequestCallback(callback));
        return true;
    }

    private RequestCallback getInsertCarIssueRequestCallback(CarIssueInsertCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarIssueAdded();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public synchronized boolean updateCarIssue(CarIssue model, CarIssueUpdateCallback callback) {
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

    private RequestCallback getUpdateCarIssueRequestCallback(CarIssueUpdateCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onCarIssueUpdated();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public synchronized void getUpcomingCarIssues(int carId, CarIssueGetUpcomingCallback callback){
        networkHelper.getUpcomingCarIssues(carId,getUpcomingCarIssuesRequestCallback(callback));
    }

    private synchronized RequestCallback getUpcomingCarIssuesRequestCallback(CarIssueGetUpcomingCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    Timeline timelineData = new Gson().fromJson(response, Timeline.class);
                    List<UpcomingIssue> issues = timelineData.getResults().get(DEALERSHIP_ISSUES)
                            .getUpcomingIssues();
                    callback.onCarIssueGotUpcoming(issues);
                }
                else{
                    callback.onError();
                }
            }
        };

        return requestCallback;
    }

    public synchronized void getCurrentCarIssues(int carId, CarIssueGetCurrentCallback callback){
        networkHelper.getCurrentCarIssues(carId,getCurrentCarIssuesRequestCallback(carId,callback));
    }

    private synchronized RequestCallback getCurrentCarIssuesRequestCallback(final int carId, CarIssueGetCurrentCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("results");

                        ArrayList<CarIssue> carIssues = CarIssue.createCarIssues(jsonArray,carId);
                        callback.onCarIssueGotCurrent(carIssues);
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){
                    callback.onError();
                }
            }
        };

        return requestCallback;
    }

    public synchronized List<CarIssue> getDoneCarIssues(int carId, CarIssueGetDoneCallback callback){
        networkHelper.getDoneCarIssues(carId,getDoneCarIssuesRequestCallback(carId,callback));
        return carIssueAdapter.getAllDoneCarIssues();
    }

    private RequestCallback getDoneCarIssuesRequestCallback(final int carId, CarIssueGetDoneCallback callback){
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

                        callback.onCarIssueGotDone(carIssues);
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){

                }
            }
        };

        return requestCallback;
    }
}

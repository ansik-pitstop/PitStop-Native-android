package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

//MAY BE USELESS FOR NOW SINCE CAR ISSUES ARE TIED TO CAR REPOSITORY ANYWAY, NOT IMPLEMENTED FULLY

/**
 * CarIssue repository, use this class to modify, retrieve, and delete car issue data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

public class CarIssueRepository {

    private static CarIssueRepository INSTANCE;
    private LocalCarIssueAdapter carIssueAdapter;
    private NetworkHelper networkHelper;

    interface CarIssueInsertCallback{
        void onCarIssueAdded();
        void onError();
    }

    interface CarIssueUpdateCallback{
        void onCarIssueUpdated();
        void onError();
    }

    interface CarIssueGetUpcomingCallback{
        void onCarIssueGotUpcoming(List<CarIssue> carIssueUpcoming);
        void onError();
    }

    interface CarIssueGetCurrentCallback{
        void onCarIssueGotCurrent(List<CarIssue> carIssueCurrent);
        void onError();
    }

    interface CarIssueGetDoneCallback{
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

    public boolean insert(CarIssue model, RequestCallback callback) {
        carIssueAdapter.storeCarIssue(model);
        networkHelper.postUserInputIssue(model.getCarId(),model.getItem(),model.getAction()
                ,model.getDescription(),model.getPriority(),callback);
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

    public boolean update(CarIssue model, RequestCallback callback) {
        carIssueAdapter.updateCarIssue(model);

        if (model.getStatus().equals(CarIssue.ISSUE_DONE)){
            networkHelper.setIssueDone(model.getCarId(),model.getId(),model.getDaysAgo()
                    ,model.getDoneMileage(),callback);
        }
        else if (model.getStatus().equals(CarIssue.ISSUE_PENDING)){
            networkHelper.setIssuePending(model.getCarId(),model.getId(),callback);
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

    public List<CarIssue> getUpcomingCarIssues(int carId, RequestCallback callback){
        networkHelper.getUpcomingCarIssues(carId,callback);
        return carIssueAdapter.getAllUpcomingCarIssues();
    }

//    private RequestCallback getUpcomingCarIssuesRequestCallback(CarIssueGetUpcomingCallback callback){
//        //Create corresponding request callback
//        RequestCallback requestCallback = new RequestCallback() {
//            @Override
//            public void done(String response, RequestError requestError) {
//                try {
//                    if (requestError == null){
//                        ArrayList<CarIssue> carIssues = new ArrayList<>();
//                        JSONObject jsonObject = new JSONObject(response);
//                    }
//                    else{
//                        callback.onError();
//                    }
//                }
//                catch(JSONException e){
//
//                }
//            }
//        };
//
//        return requestCallback;
//    }

    public List<CarIssue> getCurrentCarIssues(int carId, RequestCallback callback){
        networkHelper.getCurrentCarIssues(carId,callback);
        return carIssueAdapter.getAllCurrentCarIssues();
    }

    public List<CarIssue> getDoneCarIssues(int carId, RequestCallback callback){
        networkHelper.getDoneCarIssues(carId,callback);
        return carIssueAdapter.getAllDoneCarIssues();
    }
}

package com.pitstop.repositories;

import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

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

    public List<CarIssue> getUpcomingCarIssues(int carId, RequestCallback callback){
        networkHelper.getUpcomingCarIssues(carId,callback);
        return carIssueAdapter.getAllUpcomingCarIssues();
    }

    public List<CarIssue> getCurrentCarIssues(int carId, RequestCallback callback){
        networkHelper.getCurrentCarIssues(carId,callback);
        return carIssueAdapter.getAllCurrentCarIssues();
    }

    public List<CarIssue> getDoneCarIssues(int carId, RequestCallback callback){
        networkHelper.getDoneCarIssues(carId,callback);
        return carIssueAdapter.getAllDoneCarIssues();
    }
}

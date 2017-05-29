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

public class CarIssueRepository implements Repository<CarIssue> {

    private LocalCarIssueAdapter carIssueAdapter;
    private NetworkHelper networkHelper;

    public CarIssueRepository(LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper){
        this.carIssueAdapter = localCarIssueAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public boolean insert(CarIssue model, RequestCallback callback) {
        carIssueAdapter.storeCarIssue(model);
        networkHelper.postUserInputIssue(model.getCarId(),model.getItem(),model.getAction()
                ,model.getDescription(),model.getPriority(),callback);
        return true;
    }

    @Override
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

    @Override
    public CarIssue get(int id, RequestCallback callback) {
        //not applicable
        return null;
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

    @Override
    public boolean delete(CarIssue model, RequestCallback callback) {
        carIssueAdapter.deleteCarIssue(model);
        return false;
    }
}

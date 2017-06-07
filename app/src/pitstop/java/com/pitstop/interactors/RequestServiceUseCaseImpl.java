package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.repositories.CarIssueRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class RequestServiceUseCaseImpl implements RequestServiceUseCase {

    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private CarIssue carIssue;
    private Handler handler;

    public RequestServiceUseCaseImpl(CarIssueRepository carIssueRepository, Handler handler) {
        this.carIssueRepository = carIssueRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        carIssueRepository.insert(carIssue,new CarIssueRepository.CarIssueInsertCallback(){

                @Override
                public void onCarIssueAdded() {
                    callback.onServiceRequested();
                }

                @Override
                public void onError() {
                    callback.onError();
                }
            });
    }

    @Override
    public void execute(CarIssue carIssue, Callback callback) {
        this.callback = callback;
        this.carIssue = carIssue;
        handler.post(this);
    }
}

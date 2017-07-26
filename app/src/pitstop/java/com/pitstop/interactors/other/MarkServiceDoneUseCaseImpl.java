package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class MarkServiceDoneUseCaseImpl implements MarkServiceDoneUseCase {

    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private CarIssue carIssue;
    private Handler handler;

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository, Handler handler){
        this.carIssueRepository = carIssueRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        carIssue.setStatus(CarIssue.ISSUE_DONE);
        carIssueRepository.updateCarIssue(carIssue, new CarIssueRepository.Callback<Object>() {
            @Override
            public void onSuccess(Object response) {
                callback.onServiceMarkedAsDone();
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void execute(CarIssue carIssue, Callback callback) {
        this.carIssue = carIssue;
        this.callback = callback;
        handler.post(this);
    }
}

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
    private Handler useCaseHandler;
    private Handler mainHandler;

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServiceMarkedAsDone(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onServiceMarkedAsDone();
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    @Override
    public void run() {
        carIssue.setStatus(CarIssue.ISSUE_DONE);
        carIssueRepository.updateCarIssue(carIssue, new CarIssueRepository.Callback<Object>() {
            @Override
            public void onSuccess(Object response) {
                MarkServiceDoneUseCaseImpl.this.onServiceMarkedAsDone();
            }

            @Override
            public void onError(RequestError error) {
                MarkServiceDoneUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(CarIssue carIssue, Callback callback) {
        this.carIssue = carIssue;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

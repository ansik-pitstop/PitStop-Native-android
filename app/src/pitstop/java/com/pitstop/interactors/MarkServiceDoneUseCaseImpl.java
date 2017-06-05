package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.CarIssue;
import com.pitstop.repositories.CarIssueRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class MarkServiceDoneUseCaseImpl implements MarkServiceDoneUseCase {

    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private CarIssue carIssue;

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository){
        this.carIssueRepository = carIssueRepository;
    }

    @Override
    public void run() {
        carIssue.setStatus(carIssue.ISSUE_DONE);
        carIssueRepository.updateCarIssue(carIssue, new CarIssueRepository.CarIssueUpdateCallback() {
            @Override
            public void onCarIssueUpdated() {
                callback.onServiceMarkedAsDone();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(CarIssue carIssue, Callback callback) {
        this.carIssue = carIssue;
        this.callback = callback;
        new Handler().post(this);
    }
}

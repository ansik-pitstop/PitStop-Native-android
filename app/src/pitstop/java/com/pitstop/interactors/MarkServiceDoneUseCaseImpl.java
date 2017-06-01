package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class MarkServiceDoneUseCaseImpl implements MarkServiceDoneUseCase {

    private LocalCarIssueAdapter localCarIssueAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;
    private CarIssue carIssue;

    public MarkServiceDoneUseCaseImpl(LocalCarIssueAdapter localCarAdapter, NetworkHelper networkHelper){
        this.localCarIssueAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void run() {
        carIssue.setStatus(carIssue.ISSUE_DONE);
        CarIssueRepository.getInstance(localCarIssueAdapter,networkHelper)
                .updateCarIssue(carIssue, new CarIssueRepository.CarIssueUpdateCallback() {
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

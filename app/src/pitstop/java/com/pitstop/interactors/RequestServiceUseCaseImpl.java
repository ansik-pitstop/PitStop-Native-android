package com.pitstop.interactors;

import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class RequestServiceUseCaseImpl implements RequestServiceUseCase {

    private LocalCarIssueAdapter localCarIssueAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;
    private CarIssue carIssue;

    public RequestServiceUseCaseImpl(LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper) {
        this.localCarIssueAdapter = localCarIssueAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void run() {
        CarIssueRepository.getInstance(localCarIssueAdapter,networkHelper)
            .insert(carIssue,new CarIssueRepository.CarIssueInsertCallback(){

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
        new Thread(this).start();
    }
}

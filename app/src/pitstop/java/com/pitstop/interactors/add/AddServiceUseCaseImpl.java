package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class AddServiceUseCaseImpl implements AddServiceUseCase {

    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private CarIssue carIssue;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public AddServiceUseCaseImpl(CarIssueRepository carIssueRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServiceRequested(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onServiceRequested();
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
        carIssueRepository.insert(carIssue,new CarIssueRepository.Callback<Object>(){

                @Override
                public void onSuccess(Object response) {
                    AddServiceUseCaseImpl.this.onServiceRequested();
                }

                @Override
                public void onError(RequestError error) {
                    AddServiceUseCaseImpl.this.onError(error);
                }
            });
    }

    @Override
    public void execute(CarIssue carIssue, Callback callback) {
        this.callback = callback;
        this.carIssue = carIssue;
        useCaseHandler.post(this);
    }
}

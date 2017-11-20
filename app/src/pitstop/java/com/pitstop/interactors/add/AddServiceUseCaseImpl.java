package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class AddServiceUseCaseImpl implements AddServiceUseCase {

    private final String TAG = getClass().getSimpleName();

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
        Logger.getInstance().logI(TAG,"Use case finished result: service requested successfully"
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onServiceRequested());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG,"Use case returned error: err="+error
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
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
        Logger.getInstance().logI(TAG,"Use case execution started input: carIssue="+carIssue
                ,false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.carIssue = carIssue;
        useCaseHandler.post(this);
    }
}

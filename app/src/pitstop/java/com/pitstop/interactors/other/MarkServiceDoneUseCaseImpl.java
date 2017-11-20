package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.EventBus.EventBusNotifier;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class MarkServiceDoneUseCaseImpl implements MarkServiceDoneUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private CarIssue carIssue;
    private EventSource eventSource;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServiceMarkedAsDone(CarIssue carIssue){
        Logger.getInstance().logI(TAG,"Use case finished: service marked as done carIssue="+carIssue
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> {
            EventBusNotifier.notifyCarDataChanged(
                    new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY), eventSource);
            callback.onServiceMarkedAsDone(carIssue);
        });
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG,"Use case returned error: err="+error
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        carIssue.setStatus(CarIssue.ISSUE_DONE);
        carIssueRepository.updateCarIssue(carIssue, new CarIssueRepository.Callback<CarIssue>() {
            @Override
            public void onSuccess(CarIssue carIssueReturned) {
                carIssue.setDoneAt(carIssueReturned.getDoneAt());
                carIssue.setStatus(carIssueReturned.getStatus());
                MarkServiceDoneUseCaseImpl.this.onServiceMarkedAsDone(carIssue);
            }

            @Override
            public void onError(RequestError error) {
                MarkServiceDoneUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(CarIssue carIssue, EventSource eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: carIssue="+carIssue
                ,false, DebugMessage.TYPE_USE_CASE);
        this.carIssue = carIssue;
        this.eventSource = eventSource;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

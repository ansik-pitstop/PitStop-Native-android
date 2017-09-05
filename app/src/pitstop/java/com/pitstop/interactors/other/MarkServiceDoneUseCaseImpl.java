package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.EventBus.EventBusNotifier;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
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
    private EventSource eventSource;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServiceMarkedAsDone(){
        mainHandler.post(() -> {
            EventBusNotifier.notifyCarDataChanged(
                    new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY), eventSource);
            callback.onServiceMarkedAsDone();
        });
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
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
    public void execute(CarIssue carIssue, EventSource eventSource, Callback callback) {
        this.carIssue = carIssue;
        this.eventSource = eventSource;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

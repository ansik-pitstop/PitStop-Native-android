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

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public MarkServiceDoneUseCaseImpl(CarIssueRepository carIssueRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServiceMarkedAsDone(CarIssue carIssue){
        Logger.getInstance().logI(TAG,"Use case finished: service marked as done carIssue="+carIssue
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> {
            EventBusNotifier.notifyCarDataChanged(
                    new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY), eventSource);
            callback.onServiceMarkedAsDone(carIssue);
        });
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        carIssue.setStatus(CarIssue.ISSUE_DONE);
        Disposable d = carIssueRepository.markDone(carIssue)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(next -> {
                    carIssue.setDoneAt(next.getDoneAt());
                    carIssue.setStatus(next.getStatus());
                    MarkServiceDoneUseCaseImpl.this.onServiceMarkedAsDone(carIssue);
                }, error -> {
                    error.printStackTrace();
                    MarkServiceDoneUseCaseImpl.this.onError(new RequestError(error));
                });
        compositeDisposable.add(d);
    }

    @Override
    public void execute(CarIssue carIssue, EventSource eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: carIssue="+carIssue
                , DebugMessage.TYPE_USE_CASE);
        this.carIssue = carIssue;
        this.eventSource = eventSource;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matt on 2017-07-31.
 */

public class AddCustomServiceUseCaseImpl implements AddCustomServiceUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarIssueRepository carIssueRepository;
    private CarRepository carRepository;
    private UserRepository userRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;

    private CarIssue issue;

    private EventSource eventSource;

    public AddCustomServiceUseCaseImpl(CarRepository carRepository,UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(CarIssue issue, EventSource eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started input: issue="+issue, DebugMessage.TYPE_USE_CASE);
        this.eventSource = eventSource;
        this.issue = issue;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onIssueAdded(CarIssue data){
        Logger.getInstance().logI(TAG,"Use case finished: result="+data, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onIssueAdded(data));
    }

    private void onError(RequestError requestError){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+requestError, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(requestError));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                carIssueRepository.insertCustom(data.getCarId(), data.getUserId(), issue, new Repository.Callback<CarIssue>() {
                    @Override
                    public void onSuccess(CarIssue data) {

                        EventType eventType = new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        AddCustomServiceUseCaseImpl.this.onIssueAdded(data);
                    }

                    @Override
                    public void onError(RequestError error) {
                        AddCustomServiceUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                AddCustomServiceUseCaseImpl.this.onError(error);

            }
        });
    }
}

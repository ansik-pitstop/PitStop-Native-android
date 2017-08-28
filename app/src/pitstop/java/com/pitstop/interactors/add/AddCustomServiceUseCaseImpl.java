package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matt on 2017-07-31.
 */

public class AddCustomServiceUseCaseImpl implements AddCustomServiceUseCase {
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
    public void execute(CarIssue issue, String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.issue = issue;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onIssueAdded(CarIssue data){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onIssueAdded(data);
            }
        });
    }

    private void onError(RequestError requestError){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(requestError);
            }
        });
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                carIssueRepository.insertCustom(data.getCarId(), data.getUserId(), issue, new Repository.Callback<CarIssue>() {
                    @Override
                    public void onSuccess(CarIssue data) {

                        EventType eventType = new EventTypeImpl(EventType.EVENT_SERVICES_NEW);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        onIssueAdded(data);
                    }

                    @Override
                    public void onError(RequestError error) {
                        onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                onError(error);

            }
        });
    }
}

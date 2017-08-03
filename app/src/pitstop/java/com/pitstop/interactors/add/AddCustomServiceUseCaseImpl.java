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
    private Handler handler;
    private Callback callback;

    private CarIssue issue;

    private EventSource eventSource;

    public AddCustomServiceUseCaseImpl(CarRepository carRepository,UserRepository userRepository, CarIssueRepository carIssueRepository, Handler handler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(CarIssue issue, String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.issue = issue;
        this.callback = callback;
        handler.post(this);
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
                        callback.onIssueAdded(data);
                    }

                    @Override
                    public void onError(RequestError error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);

            }
        });
    }
}

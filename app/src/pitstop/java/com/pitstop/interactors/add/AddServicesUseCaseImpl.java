package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by Matthew on 2017-07-17.
 */

public class AddServicesUseCaseImpl implements AddServicesUseCase {

    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private Callback callback;
    private List<CarIssue> carIssues;
    private Handler handler;

    private EventSource eventSource;

    public AddServicesUseCaseImpl(CarIssueRepository carIssueRepository, UserRepository userRepository, Handler handler) {
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                carIssueRepository.insert(data.getCarId(),carIssues,new CarIssueRepository.CarIssueInsertCallback(){

                    @Override
                    public void onCarIssueAdded() {
                        EventType eventType = new EventTypeImpl(EventType.EVENT_SERVICES_NEW);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        callback.onServicesAdded();
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });

    }

    @Override
    public void execute(List<CarIssue> carIssues, String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carIssues = carIssues;
        handler.post(this);
    }
}

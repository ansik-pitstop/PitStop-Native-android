package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by Matthew on 2017-07-17.
 */

public class AddServicesUseCaseImpl implements AddServicesUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private Callback callback;
    private List<CarIssue> carIssues;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private EventSource eventSource;

    public AddServicesUseCaseImpl(CarIssueRepository carIssueRepository, UserRepository userRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServicesAdded(){
        Logger.getInstance().logI(TAG,"Use case finished result: service added", DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onServicesAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                carIssueRepository.insert(data.getCarId(),carIssues,new CarIssueRepository.Callback<Object>(){

                    @Override
                    public void onSuccess(Object response) {
                        EventType eventType = new EventTypeImpl(EventType.EVENT_SERVICES_NEW);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        AddServicesUseCaseImpl.this.onServicesAdded();
                    }

                    @Override
                    public void onError(RequestError error) {
                        AddServicesUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                AddServicesUseCaseImpl.this.onError(error);
            }
        });

    }

    @Override
    public void execute(List<CarIssue> carIssues, String eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started input: carIssues="+carIssues
                , DebugMessage.TYPE_USE_CASE);
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carIssues = carIssues;
        useCaseHandler.post(this);
    }
}

package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.User;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private UserRepository userRepository;
    private int carId;
    private Callback callback;
    private Handler handler;

    private EventSource eventSource;

    public SetUserCarUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                userRepository.setUserCar(user.getId(), carId, new UserRepository.UserSetCarCallback() {
                    @Override
                    public void onSetCar() {
                        EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        callback.onUserCarSet();
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });

    }

    @Override
    public void execute(int carId,String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carId = carId;
        handler.post(this);
    }
}

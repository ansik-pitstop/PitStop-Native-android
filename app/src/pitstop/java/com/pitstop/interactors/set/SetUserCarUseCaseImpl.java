package com.pitstop.interactors.set;

import android.os.Handler;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private int carId;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private EventSource eventSource;

    public SetUserCarUseCaseImpl(UserRepository userRepository, Handler useCaseHandler
            , Handler mainHandler) {
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onUserCarSet(){
        mainHandler.post(() -> callback.onUserCarSet());
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void execute(int carId,String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carId = carId;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        Log.d(TAG,"run() carId: "+carId+", eventSource: "+eventSource.getSource());
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG,"current user: "+user);
                userRepository.setUserCar(user.getId(), carId, new Repository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object object) {
                        EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                ,eventSource));
                        SetUserCarUseCaseImpl.this.onUserCarSet();
                    }

                    @Override
                    public void onError(RequestError error) {
                        SetUserCarUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                SetUserCarUseCaseImpl.this.onError(error);
            }
        });

    }
}

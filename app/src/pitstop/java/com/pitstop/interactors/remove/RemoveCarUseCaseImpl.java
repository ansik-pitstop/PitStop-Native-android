package com.pitstop.interactors.remove;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

/*
Removes the provided car, if the car was the users current car it will
set the users current car to the next most recent car
 */

public class RemoveCarUseCaseImpl implements RemoveCarUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private int carToDeleteId;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;

    private EventSource eventSource;

    public RemoveCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler){
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(int carToDeleteId,String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.carToDeleteId = carToDeleteId;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onCarRemoved(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onCarRemoved();
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    @Override
    public void run() {

        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                carRepository.delete(carToDeleteId, new CarRepository.Callback<Object>() {

                    @Override
                    public void onSuccess(Object response) {
                        if(data.getCarId() == carToDeleteId){// deleted the users current car
                            userRepository.getCurrentUser(new Repository.Callback<User>() {

                                @Override
                                public void onSuccess(User user) {
                                    carRepository.getCarsByUserId(user.getId(), new Repository.Callback<List<Car>>() {

                                        @Override
                                        public void onSuccess(List<Car> cars) {
                                            if(cars.size()>0){//does the user have another car?
                                                userRepository.setUserCar(user.getId(), cars.get(cars.size() - 1).getId(), new Repository.Callback<Object>() {

                                                    @Override
                                                    public void onSuccess(Object object) {
                                                        EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                                                ,eventSource));
                                                        RemoveCarUseCaseImpl.this.onCarRemoved();
                                                    }

                                                    @Override
                                                    public void onError(RequestError error) {
                                                        RemoveCarUseCaseImpl.this.onError(error);
                                                    }
                                                });
                                            }else{//user doesn't have another car
                                                networkHelper.setNoMainCar(user.getId(), new RequestCallback() {
                                                    @Override
                                                    public void done(String response, RequestError requestError) {
                                                        if(requestError ==null){
                                                            EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                                            EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                                                    ,eventSource));
                                                            RemoveCarUseCaseImpl.this.onCarRemoved();
                                                        }else{
                                                            RemoveCarUseCaseImpl.this.onError(requestError);
                                                        }
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onError(RequestError error) {
                                            RemoveCarUseCaseImpl.this.onError(error);
                                        }
                                    });
                                }

                                @Override
                                public void onError(RequestError error) {
                                    RemoveCarUseCaseImpl.this.onError(error);
                                }
                            });
                        }else{
                            RemoveCarUseCaseImpl.this.onCarRemoved();
                        }
                    }

                    @Override
                    public void onError(RequestError error) {
                        RemoveCarUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                RemoveCarUseCaseImpl.this.onError(error);
            }
        });
    }
}

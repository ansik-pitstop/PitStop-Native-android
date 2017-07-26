package com.pitstop.interactors.update;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matthew on 2017-06-20.
 */

public class UpdateCarDealershipUseCaseImpl implements UpdateCarDealershipUseCase {
    private Handler handler;
    private CarRepository carRepository;
    private UserRepository userRepository;
    private UpdateCarDealershipUseCase.Callback callback;

    private EventSource eventSource;

    private int carId;
    private Dealership dealership;

    public UpdateCarDealershipUseCaseImpl(CarRepository carRepository, UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(int carId, Dealership dealership, String eventSource, UpdateCarDealershipUseCase.Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carId = carId;
        this.dealership = dealership;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                carRepository.get(carId,user.getId(), new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        car.setDealership(dealership);
                        carRepository.update(car, new Repository.Callback<Object>() {
                            @Override
                            public void onSuccess(Object response) {

                                EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP);
                                EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                        ,eventSource));
                                callback.onCarDealerUpdated();
                            }

                            @Override
                            public void onError(RequestError error) {
                                callback.onError();
                            }
                        });
                    }
                    @Override
                    public void onError(RequestError error) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                callback.onError();

            }
        });
    }
}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matthew on 2017-06-20.
 */

public class GetCarByCarIdUseCaseImpl implements GetCarByCarIdUseCase {
    private CarRepository carRepository;

    private UserRepository userRepository;
    private Callback callback;
    private Handler handler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, UserRepository userRepository, Handler handler) {
        this.handler = handler;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void execute(int carId,Callback callback) {
        this.callback = callback;
        this.carId = carId;
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
                        callback.onCarGot(car);
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

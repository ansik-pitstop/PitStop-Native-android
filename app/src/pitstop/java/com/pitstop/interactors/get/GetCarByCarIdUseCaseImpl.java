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
    private Handler useCaseHandler;
    private Handler mainHandler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, UserRepository userRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(int carId,Callback callback) {
        this.callback = callback;
        this.carId = carId;
        useCaseHandler.post(this);
    }

    private void onCarGot(Car car){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onCarGot(car);
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
    };

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                carRepository.get(carId,user.getId(), new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        GetCarByCarIdUseCaseImpl.this.onCarGot(car);
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetCarByCarIdUseCaseImpl.this.onError(error);
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                GetCarByCarIdUseCaseImpl.this.onError(error);
            }
        });
    }
}

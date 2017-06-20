package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xirax on 2017-06-13.
 */

public class GetCarsByUserIdUseCaseImpl implements GetCarsByUserIdUseCase {
    private Handler handler;
    private CarRepository carRepository;
    private UserRepository userRepository;

    private GetCarsByUserIdUseCase.Callback callback;


    public GetCarsByUserIdUseCaseImpl(UserRepository userRepository,CarRepository carRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
        this.carRepository = carRepository;

    }

    @Override
    public void execute(GetCarsByUserIdUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {

            @Override
            public void onGotCar(Car car) {

                final Car userCar = car;

                userRepository.getCurrentUser(new UserRepository.UserGetCallback(){

                    @Override
                    public void onGotUser(User user) {

                        carRepository.getCarByUserId(user.getId(),new CarRepository.CarsGetCallback() {

                            @Override
                            public void onCarsGot(List<Car> cars) {

                                for (Car c: cars){
                                    if (c.getId() == userCar.getId()){
                                        c.setCurrentCar(true);
                                    }
                                    else{
                                        c.setCurrentCar(false);
                                    }
                                }
                                callback.onCarsRetrieved(cars);
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
            public void onNoCarSet() {
                ArrayList<Car> cars = new ArrayList<>();
                callback.onCarsRetrieved(cars);
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

}

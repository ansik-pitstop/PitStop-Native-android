package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matt on 2017-06-13.
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
                userRepository.getCurrentUser(new UserRepository.UserGetCallback(){
                    @Override
                    public void onGotUser(User user) {
                        carRepository.getCarsByUserId(user.getId(),new CarRepository.CarsGetCallback() {
                            @Override
                            public void onCarsGot(List<Car> cars) {
                                callback.onCarsRetrieved(cars);
                            }

                            @Override
                            public void onNoCarsGot(List<Car> cars) {

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
                callback.onCarsRetrieved(new ArrayList<Car>());
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });




    }
}

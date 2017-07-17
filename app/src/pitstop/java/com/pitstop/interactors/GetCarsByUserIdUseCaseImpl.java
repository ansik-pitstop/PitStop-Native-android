package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

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
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()) {
                    callback.onCarsRetrieved(new ArrayList<Car>());
                    return;
                }

                userRepository.getCurrentUser(new UserRepository.UserGetCallback(){
                    @Override
                    public void onGotUser(User user) {
                        carRepository.getCarsByUserId(user.getId()
                                ,new Repository.Callback<List<Car>>() {

                            @Override
                            public void onSuccess(List<Car> cars) {
                                callback.onCarsRetrieved(cars);
                            }
                            @Override
                            public void onError(int error) {
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
            public void onError(int error) {
                callback.onError();
            }
        });

    }
}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private UserRepository userRepository;
    private CarRepository carRepository;
    private Callback callback;
    private Handler handler;

    public GetUserCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , Handler handler) {

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                if (!data.hasMainCar()) {
                    callback.onNoCarSet();
                    return;
                }

                carRepository.get(data.getCarId(), data.getUserId(), new CarRepository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        callback.onCarRetrieved(car);
                    }

                    @Override
                    public void onError(RequestError error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}

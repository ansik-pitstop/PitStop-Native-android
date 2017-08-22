package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

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
            public void onSuccess(Settings userSettings) {

                //Main car is stored in user settings, retrieve it from there
                if (userSettings.hasMainCar()){
                    carRepository.get(userSettings.getCarId(), userSettings.getUserId(), new CarRepository.Callback<Car>() {
                        @Override
                        public void onSuccess(Car car) {
                            callback.onCarRetrieved(car);
                        }

                        @Override
                        public void onError(RequestError error) {
                            callback.onError(error);
                        }
                    });
                    return;
                }

                /*User settings doesn't have mainCar stored, we cannot trust this because settings
                ** could potentially be corrupted, so perform a double-check by retrieving cars*/
                carRepository.getCarsByUserId(userSettings.getUserId(), new Repository.Callback<List<Car>>() {

                    @Override
                    public void onSuccess(List<Car> carList) {
                        if (carList.isEmpty()){
                            callback.onNoCarSet();
                        }
                        else{
                            callback.onCarRetrieved(carList.get(0));

                            //Fix corrupted user settings
                            userRepository.setUserCar(userSettings.getUserId(), carList.get(0).getId()
                                    , new Repository.Callback<Object>() {
                                @Override
                                public void onSuccess(Object response){}
                                @Override
                                public void onError(RequestError error){}
                            });
                        }
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

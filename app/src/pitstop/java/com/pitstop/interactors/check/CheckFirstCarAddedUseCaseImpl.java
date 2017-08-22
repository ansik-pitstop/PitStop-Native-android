package com.pitstop.interactors.check;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 *
 * Created by Karol Zdebel on 6/8/2017.
 */

public class CheckFirstCarAddedUseCaseImpl implements CheckFirstCarAddedUseCase {

    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler handler;
    private Callback callback;


    public CheckFirstCarAddedUseCaseImpl(UserRepository userRepository, CarRepository carRepository
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

                //If everything went right during the add car process, this should return true
                if (userSettings.isFirstCarAdded()){
                    callback.onFirstCarAddedChecked(true);
                    return;
                }

                //We only get here if something bad happened in add car, or user settings were corrupted
                carRepository.getCarsByUserId(userSettings.getUserId(), new Repository.Callback<List<Car>>() {

                    @Override
                    public void onSuccess(List<Car> cars) {
                        if (cars.isEmpty()){
                            callback.onFirstCarAddedChecked(false);
                            return;
                        }

                        //First car is added, fix the settings because they must be corrupted

                        //Step 1: Set firstCarAdded=true
                        userRepository.setFirstCarAdded(true, new Repository.Callback<Object>() {
                            @Override
                            public void onSuccess(Object response){

                                //Step 2: Set mainCar=cars[0].carId
                                userRepository.setUserCar(userSettings.getUserId()
                                        , cars.get(0).getId(), new Repository.Callback<Object>() {

                                        @Override
                                        public void onSuccess(Object response){
                                            callback.onFirstCarAddedChecked(true);
                                        }

                                        @Override
                                        public void onError(RequestError error){
                                            callback.onError(error);
                                        }
                                });
                            }
                            @Override
                            public void onError(RequestError error){
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

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}

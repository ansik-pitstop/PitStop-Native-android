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
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;


    public CheckFirstCarAddedUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onFirstCarAddedChecked(boolean added){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFirstCarAddedChecked(added);
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
            public void onSuccess(Settings userSettings) {

                //If everything went right during the add car process, this should return true
                if (userSettings.isFirstCarAdded()){
                    CheckFirstCarAddedUseCaseImpl.this.onFirstCarAddedChecked(true);
                    return;
                }

                //We only get here if something bad happened in add car, or user settings were corrupted
                carRepository.getCarsByUserId(userSettings.getUserId(), new Repository.Callback<List<Car>>() {

                    @Override
                    public void onSuccess(List<Car> cars) {
                        if (cars.isEmpty()){
                            CheckFirstCarAddedUseCaseImpl.this.onFirstCarAddedChecked(false);
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
                                            CheckFirstCarAddedUseCaseImpl.this.onFirstCarAddedChecked(true);
                                        }

                                        @Override
                                        public void onError(RequestError error){
                                            CheckFirstCarAddedUseCaseImpl.this.onError(error);
                                        }
                                });
                            }
                            @Override
                            public void onError(RequestError error){
                                CheckFirstCarAddedUseCaseImpl.this.onError(error);
                            }
                        });
                    }

                    @Override
                    public void onError(RequestError error) {
                        CheckFirstCarAddedUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                CheckFirstCarAddedUseCaseImpl.this.onError(error);
            }
        });
    }
}

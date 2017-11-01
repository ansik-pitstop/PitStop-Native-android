package com.pitstop.interactors.update;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class UpdateCarMileageUseCaseImpl implements UpdateCarMileageUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private Handler usecaseHandler;
    private Handler mainHandler;

    private Callback callback;
    private double mileage;

    public UpdateCarMileageUseCaseImpl(CarRepository carRepository, UserRepository userRepository
            , Handler usecaseHandler, Handler mainHandler) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.usecaseHandler = usecaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(double mileage, Callback callback){
        this.callback = callback;
        this.mileage = mileage;
        usecaseHandler.post(this);
    }

    private void onMileageUpdated(){
        mainHandler.post(() -> callback.onMileageUpdated());
    }

    private void onNoCarAdded(){
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings settings) {

                if (!settings.hasMainCar()){
                    UpdateCarMileageUseCaseImpl.this.onNoCarAdded();
                    return;
                }

                carRepository.get(settings.getCarId(), new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        car.setTotalMileage(mileage);
                        carRepository.update(car, new Repository.Callback<Object>() {

                            @Override
                            public void onSuccess(Object response){
                                UpdateCarMileageUseCaseImpl.this.onMileageUpdated();
                            }

                            @Override
                            public void onError(RequestError error){
                                UpdateCarMileageUseCaseImpl.this.onError(error);
                            }
                        });
                    }

                    @Override
                    public void onError(RequestError error) {
                        UpdateCarMileageUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                UpdateCarMileageUseCaseImpl.this.onError(error);
            }
        });
    }
}

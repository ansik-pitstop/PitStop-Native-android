package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.repositories.CarRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class AddCarUseCaseImpl implements AddCarUseCase {

    private CarRepository carRepository;
    private Car car;
    private Callback callback;

    public AddCarUseCaseImpl(CarRepository carRepository){
        this.carRepository = carRepository;
    }

    @Override
    public void execute(Car car, Callback callback) {
        this.car = car;
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        carRepository.insert(car, new CarRepository.CarInsertCallback() {
            @Override
            public void onCarAdded() {
                callback.onCarAdded();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 9/25/2017.
 */

public class GetCarByVinUseCaseImpl implements GetCarByVinUseCase {

    private Handler useCaseHandler;
    private Handler mainHandler;
    private CarRepository carRepository;
    private Callback callback;
    private String vin;

    public GetCarByVinUseCaseImpl(Handler useCaseHandler, Handler mainHandler, CarRepository carRepository) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.carRepository = carRepository;
    }

    @Override
    public void execute(String vin, Callback callback) {
        this.vin = vin;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        carRepository.getCarByVin(vin, new Repository.Callback<Car>() {
            @Override
            public void onSuccess(Car car) {
                if (car == null){
                    GetCarByVinUseCaseImpl.this.onNoCarFound();
                    return;
                }
                GetCarByVinUseCaseImpl.this.onGotCar(car);
            }

            @Override
            public void onError(RequestError error) {
                GetCarByVinUseCaseImpl.this.onError(error);
            }
        });
    }

    private void onGotCar(Car car){
        mainHandler.post(() -> callback.onGotCar(car));
    }

    private void onNoCarFound(){
        mainHandler.post(() -> callback.onNoCarFound());
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 9/25/2017.
 */

public class GetCarByVinUseCaseImpl implements GetCarByVinUseCase {

    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private CarRepository carRepository;
    private UserRepository userRepository;
    private Callback callback;
    private String vin;

    public GetCarByVinUseCaseImpl(Handler useCaseHandler, Handler mainHandler, CarRepository carRepository, UserRepository userRepository) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void execute(String vin, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: vin="+vin
                , DebugMessage.TYPE_USE_CASE);
        this.vin = vin;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User data) {
                carRepository.getCarByVin("" + data.getId(), vin, new Repository.Callback<Car>() {
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

            @Override
            public void onError(RequestError error) {
                GetCarByVinUseCaseImpl.this.onError(error);
            }
        });
    }

    private void onGotCar(Car car){
        Logger.getInstance().logI(TAG,"Use case finished result: car="+car
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onGotCar(car));
    }

    private void onNoCarFound(){
        Logger.getInstance().logI(TAG,"Use case finished result: no car found!"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoCarFound());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

}

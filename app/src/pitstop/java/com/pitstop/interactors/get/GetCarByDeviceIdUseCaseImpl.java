package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.ObdScanner;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class GetCarByDeviceIdUseCaseImpl implements GetCarByDeviceIdUseCase {

    private UserRepository userRepository;
    private CarRepository carRepository;
    private ScannerRepository scannerRepository;
    private Handler handler;
    private String deviceId;
    private Callback callback;

    public GetCarByDeviceIdUseCaseImpl(UserRepository userRepository
            , CarRepository carRepository, ScannerRepository scannerRepository, Handler handler){

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.scannerRepository = scannerRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String deviceId, Callback callback) {
        this.deviceId = deviceId;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {

            @Override
            public void onSuccess(User user) {
                scannerRepository.getScanner(deviceId, new Repository.Callback<ObdScanner>() {

                    @Override
                    public void onSuccess(ObdScanner obdScanner) {
                        carRepository.get(obdScanner.getCarId()).doOnNext(response -> {
                            if (response.getData() == null) callback.onNoCarFound();
                            else callback.onGotCar(response.getData());
                        }).onErrorReturn(err -> new RepositoryResponse<>(null,false))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
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

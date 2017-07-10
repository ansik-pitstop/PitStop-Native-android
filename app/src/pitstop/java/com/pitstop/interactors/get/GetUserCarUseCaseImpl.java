package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private UserRepository userRepository;
    private NetworkHelper networkHelper;
    private Callback callback;
    private Handler handler;

    public GetUserCarUseCaseImpl(UserRepository userRepository, NetworkHelper networkHelper, Handler handler) {
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {
                callback.onCarRetrieved(car);
            }

            @Override
            public void onNoCarSet() {
                callback.onNoCarSet();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}

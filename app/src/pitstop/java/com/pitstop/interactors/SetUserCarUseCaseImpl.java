package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private UserRepository userRepository;
    private int carId;
    private int userId;
    private Callback callback;
    private Handler handler;

    public SetUserCarUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.setUserCar(userId, carId, new UserRepository.UserSetCarCallback() {
            @Override
            public void onSetCar() {
                callback.onUserCarSet();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(int userId, int carId, Callback callback) {
        this.callback = callback;
        this.userId = userId;
        this.carId = carId;
        handler.post(this);
    }
}

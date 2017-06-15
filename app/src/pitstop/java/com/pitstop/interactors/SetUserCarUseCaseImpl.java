package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private UserRepository userRepository;
    private int carId;
    private Callback callback;
    private Handler handler;

    public SetUserCarUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                userRepository.setUserCar(user.getId(), carId, new UserRepository.UserSetCarCallback() {
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
            public void onError() {
                callback.onError();
            }
        });

    }

    @Override
    public void execute(int carId, Callback callback) {
        this.callback = callback;
        this.carId = carId;
        handler.post(this);
    }
}

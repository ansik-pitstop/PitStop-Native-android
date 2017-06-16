package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.repositories.UserRepository;

/**
 * Created by xirax on 2017-06-15.
 */

public class UpdateUserPhoneUseCaseImpl implements UpdateUserPhoneUseCase {
    private Handler handler;
    private UserRepository userRepository;
    private UpdateUserPhoneUseCase.Callback callback;
    private String phone;


    public UpdateUserPhoneUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;

    }

    @Override
    public void execute(String phone, UpdateUserPhoneUseCase.Callback callback) {
        this.callback = callback;
        this.phone = phone;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                user.setPhone(phone);
                userRepository.update(user, new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onUpdatedUser() {
                        callback.onUserPhoneUpdated();
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
}

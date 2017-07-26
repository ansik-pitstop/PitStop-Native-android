package com.pitstop.interactors.update;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matt on 2017-06-15.
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
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                user.setPhone(phone);
                userRepository.update(user, new Repository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object object) {
                        callback.onUserPhoneUpdated();
                    }
                    @Override
                    public void onError(RequestError error) {
                        callback.onError();

                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                callback.onError();
            }
        });

    }
}

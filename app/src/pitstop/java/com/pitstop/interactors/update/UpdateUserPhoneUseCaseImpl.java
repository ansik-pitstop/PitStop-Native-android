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
    private Handler useCaseHandler;
    private Handler mainHandler;
    private UserRepository userRepository;
    private UpdateUserPhoneUseCase.Callback callback;
    private String phone;


    public UpdateUserPhoneUseCaseImpl(UserRepository userRepository, Handler useCaseHandler
            , Handler mainHandler) {
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(String phone, UpdateUserPhoneUseCase.Callback callback) {
        this.callback = callback;
        this.phone = phone;
        useCaseHandler.post(this);
    }

    private void onUserPhoneUpdated(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onUserPhoneUpdated();
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
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
                        UpdateUserPhoneUseCaseImpl.this.onUserPhoneUpdated();
                    }
                    @Override
                    public void onError(RequestError error) {
                        UpdateUserPhoneUseCaseImpl.this.onError(error);

                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                UpdateUserPhoneUseCaseImpl.this.onError(error);
            }
        });

    }
}

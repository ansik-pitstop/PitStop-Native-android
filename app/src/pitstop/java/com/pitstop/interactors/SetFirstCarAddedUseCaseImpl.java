package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class SetFirstCarAddedUseCaseImpl implements SetFirstCarAddedUseCase {

    private UserRepository userRepository;
    private Callback callback;
    private boolean sent;

    public SetFirstCarAddedUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(boolean sent, Callback callback) {
        this.sent = sent;
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        userRepository.setFirstCarAdded(sent
                , new UserRepository.UserFirstCarAddedSetCallback() {

            @Override
            public void onFirstCarAddedSet() {
                callback.onFirstCarAddedSet();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}

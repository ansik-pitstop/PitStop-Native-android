package com.pitstop.interactors.check;

import android.os.Handler;

import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class CheckFirstCarAddedUseCaseImpl implements CheckFirstCarAddedUseCase {

    private UserRepository userRepository;
    private Handler handler;
    private Callback callback;


    public CheckFirstCarAddedUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.checkFirstCarAdded(new UserRepository.CheckFirstCarAddedCallback() {
                    @Override
                    public void onFirstCarAddedChecked(boolean added) {
                        callback.onFirstCarAddedChecked(added);
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
    }
}

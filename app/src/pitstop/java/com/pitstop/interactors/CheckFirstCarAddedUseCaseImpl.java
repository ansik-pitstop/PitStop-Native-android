package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.repositories.Repository;
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
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                callback.onFirstCarAddedChecked(data.isFirstCarAdded());
            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });
    }
}

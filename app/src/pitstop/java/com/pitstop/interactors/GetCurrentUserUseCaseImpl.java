package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matt on 2017-06-14.
 */

public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {
    private Handler handler;
    private UserRepository userRepository;

    private GetCurrentUserUseCase.Callback callback;


    public GetCurrentUserUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;

    }

    @Override
    public void execute(GetCurrentUserUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>(){
            @Override
            public void onSuccess(User user) {
                callback.onUserRetrieved(user);
            }
            @Override
            public void onError(int error) {
                callback.onError();
            }
        });




    }
}

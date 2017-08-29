package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matt on 2017-06-14.
 */

public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {
    private Handler useCaseHandler;
    private Handler mainHandler;
    private UserRepository userRepository;

    private GetCurrentUserUseCase.Callback callback;


    public GetCurrentUserUseCaseImpl(UserRepository userRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onUserRetrieved(User user){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onUserRetrieved(user);
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
    public void execute(GetCurrentUserUseCase.Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>(){
            @Override
            public void onSuccess(User user) {
                GetCurrentUserUseCaseImpl.this.onUserRetrieved(user);
            }
            @Override
            public void onError(RequestError error) {
                GetCurrentUserUseCaseImpl.this.onError(error);
            }
        });




    }
}

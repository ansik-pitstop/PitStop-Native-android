package com.pitstop.interactors.set;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class SetFirstCarAddedUseCaseImpl implements SetFirstCarAddedUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private Callback callback;
    private boolean sent;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public SetFirstCarAddedUseCaseImpl(UserRepository userRepository, Handler useCaseHandler
            , Handler mainHandler) {
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(boolean sent, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started: sent="+sent
                , DebugMessage.TYPE_USE_CASE);
        this.sent = sent;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onFirstCarAddedSet(){
        Logger.getInstance().logI(TAG, "Use case finished"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onFirstCarAddedSet());
    }
    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.setFirstCarAdded(sent
                , new Repository.Callback<Object>() {

            @Override
            public void onSuccess(Object object) {
                SetFirstCarAddedUseCaseImpl.this.onFirstCarAddedSet();
            }

            @Override
            public void onError(RequestError error) {
                SetFirstCarAddedUseCaseImpl.this.onError(error);
            }
        });
    }
}

package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class SetSmoochMessageSentUseCaseImpl implements SetSmoochMessageSentUseCase {

    private NetworkHelper networkHelper;
    private UserAdapter userAdapter;
    private Callback callback;
    private boolean sent;

    public SetSmoochMessageSentUseCaseImpl(NetworkHelper networkHelper, UserAdapter userAdapter) {
        this.networkHelper = networkHelper;
        this.userAdapter = userAdapter;
    }

    @Override
    public void execute(boolean sent, Callback callback) {
        this.sent = sent;
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        UserRepository.getInstance(userAdapter,networkHelper).setUserSmoochMessageSent(sent
                , new UserRepository.UserSetSmoochMessageSentCallback() {

            @Override
            public void onSmoochMessageSentSet() {
                callback.onUserSmoochMessageVarSet();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}

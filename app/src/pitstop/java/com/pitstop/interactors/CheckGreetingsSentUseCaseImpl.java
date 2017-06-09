package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class CheckGreetingsSentUseCaseImpl implements CheckGreetingsSentUseCase {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;


    public CheckGreetingsSentUseCaseImpl(UserAdapter userAdapter, NetworkHelper networkHelper) {
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        UserRepository.getInstance(userAdapter,networkHelper)
                .isSmoochMessageSent(new UserRepository.IsSmoochMessageSentCallback() {
                    @Override
                    public void onIsSmoochSentRetrieved(boolean sent) {
                        callback.onGotWhetherSmoochSent(sent);
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
    }
}

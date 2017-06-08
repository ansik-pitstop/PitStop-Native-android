package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.UserAdapter;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 6/8/2017.
 */

public class SetSmoochMessageSentUseCaseImpl implements SetSmoochMessageSentUseCase {

    private NetworkHelper networkHelper;
    private UserAdapter userAdapter;
    private Callback callback;
    private int userId;
    private boolean sent;

    public SetSmoochMessageSentUseCaseImpl(NetworkHelper networkHelper, UserAdapter userAdapter) {
        this.networkHelper = networkHelper;
        this.userAdapter = userAdapter;
    }

    @Override
    public void execute(boolean sent, int userId, Callback callback) {
        this.sent = sent;
        this.userId = userId;
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        
    }
}

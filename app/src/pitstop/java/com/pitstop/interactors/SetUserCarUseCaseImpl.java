package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private int carId;
    int userId;
    private Callback callback;

    public SetUserCarUseCaseImpl(UserAdapter userAdapter, NetworkHelper networkHelper) {
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void run() {
        UserRepository.getInstance(userAdapter,networkHelper)
                .setUserCar(userId, carId, new UserRepository.UserSetCarCallback() {
            @Override
            public void onSetCar() {
                callback.onUserCarSet();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(int userId, int carId, Callback callback) {
        this.callback = callback;
        this.userId = userId;
        this.carId = carId;
        new Handler().post(this);
    }
}

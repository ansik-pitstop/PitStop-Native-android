package com.pitstop.interactors;

import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class SetUserCarUseCaseImpl implements SetUserCarUseCase {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private Car car;
    private Callback callback;

    @Override
    public void run() {
        UserRepository.getInstance(userAdapter,networkHelper).
    }

    @Override
    public void execute(Car car, Callback callback) {
        this.callback = callback;
        this.car = car;
        new Thread(this).start();
    }
}

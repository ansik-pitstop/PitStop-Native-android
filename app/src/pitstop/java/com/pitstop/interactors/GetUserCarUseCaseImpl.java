package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;

    public GetUserCarUseCaseImpl(UserAdapter userAdapter, NetworkHelper networkHelper) {
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
                .getUserCar(new UserRepository.UserGetCarCallback(){

            @Override
            public void onGotCar(Car car){
                callback.onCarRetrieved(car);
            }

            @Override
            public void onError(){
                callback.onError();
            }
        });
    }
}

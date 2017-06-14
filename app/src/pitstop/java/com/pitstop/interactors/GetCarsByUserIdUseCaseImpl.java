package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.LocalCarAdapter;

import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by xirax on 2017-06-13.
 */

public class GetCarsByUserIdUseCaseImpl implements GetCarsByUserIdUseCase {
    private LocalCarAdapter localCarAdapter;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private GetCarsByUserIdUseCase.Callback callback;


    public GetCarsByUserIdUseCaseImpl(UserAdapter userAdapter, LocalCarAdapter localCarAdapter, NetworkHelper networkHelper) {
        this.userAdapter = userAdapter;
        this.localCarAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(GetCarsByUserIdUseCase.Callback callback) {
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {

        UserRepository.getInstance(userAdapter,networkHelper).getCurrentUser(new UserRepository.UserGetCallback(){
            @Override
            public void onGotUser(User user) {
                CarRepository.getInstance(localCarAdapter,networkHelper).getCarByUserId(user.getId(),new CarRepository.CarsGetCallback() {
                    @Override
                    public void onCarsGot(List<Car> cars) {
                        callback.onCarsRetrieved(cars);
                    }
                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });




    }
}

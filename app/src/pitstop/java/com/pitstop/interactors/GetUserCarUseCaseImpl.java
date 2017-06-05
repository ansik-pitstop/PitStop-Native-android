package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private UserRepository userRepository;
    private Callback callback;

    public GetUserCarUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {
        userRepository.getUserCar(new UserRepository.UserGetCarCallback(){

            @Override
            public void onGotCar(Car car){
                callback.onCarRetrieved(car);
            }

            @Override
            public void onNoCarSet() {
                callback.onNoCarSet();
            }

            @Override
            public void onError(){
                callback.onError();
            }
        });
    }
}

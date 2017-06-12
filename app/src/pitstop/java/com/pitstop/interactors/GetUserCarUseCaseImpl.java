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
    private Handler handler;

    public GetUserCarUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getUserCar(new UserRepository.UserGetCarCallback(){

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

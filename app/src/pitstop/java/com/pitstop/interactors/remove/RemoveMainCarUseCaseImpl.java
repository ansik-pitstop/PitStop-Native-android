package com.pitstop.interactors.remove;

import android.os.Handler;

import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class RemoveMainCarUseCaseImpl implements RemoveMainCarUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private Callback callback;
    private Handler handler;

    public RemoveMainCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , Handler handler){

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        
    }
}

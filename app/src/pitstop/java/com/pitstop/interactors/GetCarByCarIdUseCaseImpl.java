package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Matthew on 2017-06-20.
 */

public class GetCarByCarIdUseCaseImpl implements GetCarByCarIdUseCase {
    private CarRepository carRepository;

    private UserRepository userRepository;
    private Callback callback;
    private Handler handler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, UserRepository userRepository, Handler handler) {
        this.handler = handler;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void execute(int carId,Callback callback) {
        this.callback = callback;
        this.carId = carId;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                carRepository.get(carId,user.getId(), new CarRepository.CarGetCallback() {
                    @Override
                    public void onCarGot(Car car) {
                        callback.onCarGot(car);
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

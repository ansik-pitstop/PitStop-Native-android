package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matthew on 2017-07-17.
 */

public class RequestServiceUseCaseImpl implements RequestServiceUseCase {
    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler handler;

    private String state;
    private String timeStamp;
    private String comments;
    private Callback callback;

    public RequestServiceUseCaseImpl(CarIssueRepository carIssueRepository, UserRepository userRepository, CarRepository carRepository, Handler handler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
                    @Override
                    public void onGotCar(Car car) {
                        carIssueRepository.requestService(user.getId(), car.getId(), car.getDealership().getId(), state, timeStamp, comments, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(requestError == null && response != null){
                                    callback.onServicesRequested();
                                }else{
                                    callback.onError();
                                }
                            }
                        });
                    }

                    @Override
                    public void onNoCarSet() {
                        callback.onError();
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

    @Override
    public void execute(String state, String timeStamp, String comments, Callback callback) {
        this.state = state;
        this.timeStamp = timeStamp;
        this.comments = comments;
        this.callback = callback;
        handler.post(this);
    }
}

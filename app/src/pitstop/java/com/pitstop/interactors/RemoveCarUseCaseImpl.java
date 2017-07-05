package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

/*
Removes the provided car, if the car was the users current car it will
set the users current car to the next most recent car
 */

public class RemoveCarUseCaseImpl implements RemoveCarUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private int carToDeleteId;
    private Callback callback;
    private Handler handler;
    private NetworkHelper networkHelper;

    public RemoveCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository, NetworkHelper networkHelper, Handler handler){
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(int carToDeleteId, Callback callback) {
        this.carToDeleteId = carToDeleteId;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {

            @Override
            public void onGotCar(Car car) {
                carRepository.delete(carToDeleteId, new CarRepository.CarDeleteCallback() {

                    @Override
                    public void onCarDeleted() {
                        if(car.getId() == carToDeleteId){// deleted the users current car
                            userRepository.getCurrentUser(new UserRepository.UserGetCallback() {

                                @Override
                                public void onGotUser(User user) {
                                    carRepository.getCarsByUserId(user.getId(), new CarRepository.CarsGetCallback() {

                                        @Override
                                        public void onCarsGot(List<Car> cars) {
                                            if(cars.size()>0){//does the user have another car?
                                                userRepository.setUserCar(user.getId(), cars.get(cars.size() - 1).getId(), new UserRepository.UserSetCarCallback() {

                                                    @Override
                                                    public void onSetCar() {
                                                        callback.onCarRemoved();
                                                    }

                                                    @Override
                                                    public void onError() {
                                                        callback.onError();
                                                    }
                                                });
                                            }else{//user doesn't have another car
                                                networkHelper.setNoMainCar(user.getId(), new RequestCallback() {
                                                    @Override
                                                    public void done(String response, RequestError requestError) {
                                                        if(response != null && requestError ==null){
                                                            callback.onCarRemoved();
                                                        }else{
                                                            callback.onError();
                                                        }
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onError() {
                                            callback.onError();
                                        }

                                        @Override
                                        public void onNoCarsGot(List<Car> cars) {
                                            callback.onError();
                                        }
                                    });
                                }

                                @Override
                                public void onError() {
                                    callback.onError();
                                }
                            });
                        }else{
                            callback.onCarRemoved();
                        }
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onNoCarSet() {

            }
            @Override
            public void onError() {
                callback.onError();

            }
        });

    }
}

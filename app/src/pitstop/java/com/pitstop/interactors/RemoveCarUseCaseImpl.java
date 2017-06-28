package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

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
    private Car carToDelete;
    private Callback callback;
    private Handler handler;

    public RemoveCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository, Handler handler){
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Car carToDelete, Callback callback) {
        this.carToDelete = carToDelete;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {


        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {

            @Override
            public void onGotCar(Car car) {
                carRepository.delete(carToDelete, new CarRepository.CarDeleteCallback() {

                    @Override
                    public void onCarDeleted() {
                        if(car.getId() == carToDelete.getId()){// deleted the users current car
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
            public void onError() {
                callback.onError();

            }
        });

    }
}

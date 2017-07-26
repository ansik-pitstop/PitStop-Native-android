package com.pitstop.interactors.remove;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Matthew on 2017-06-26.
 */

public class RemoveShopUseCaseImpl implements RemoveShopUseCase {
    private NetworkHelper networkHelper;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private CarRepository carRepository;
    private RemoveShopUseCase.Callback callback;
    private Dealership dealership;
    private Handler handler;

    public RemoveShopUseCaseImpl(ShopRepository shopRepository,CarRepository carRepository, UserRepository userRepository, NetworkHelper networkHelper, Handler handler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void run() {
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                carRepository.getCarsByUserId(user.getId(), new Repository.Callback<List<Car>>() {
                    @Override
                    public void onSuccess(List<Car> cars) {
                        for(Car c : cars){
                            if(c.getDealership().getId() == dealership.getId()){
                                callback.onCantRemoveShop();
                                return;
                            }
                        }
                        shopRepository.delete(dealership.getId(), user.getId(), new Repository.Callback<Object>() {
                            @Override
                            public void onSuccess(Object response) {
                                callback.onShopRemoved();
                            }

                            @Override
                            public void onError(RequestError error) {
                                callback.onError();
                            }
                        });
                    }

                    @Override
                    public void onError(RequestError error) {
                        callback.onError();
                    }

                });
            }
            @Override
            public void onError(RequestError error) {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(Dealership dealership, RemoveShopUseCase.Callback callback) {
        this.dealership = dealership;
        this.callback = callback;
        handler.post(this);
    }

}

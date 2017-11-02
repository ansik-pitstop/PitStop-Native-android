package com.pitstop.interactors.remove;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.Response;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by Matthew on 2017-06-26.
 */

public class RemoveShopUseCaseImpl implements RemoveShopUseCase {

    private final String TAG = getClass().getSimpleName();

    private NetworkHelper networkHelper;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private CarRepository carRepository;
    private RemoveShopUseCase.Callback callback;
    private Dealership dealership;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public RemoveShopUseCaseImpl(ShopRepository shopRepository, CarRepository carRepository
            , UserRepository userRepository, NetworkHelper networkHelper
            , Handler useCaseHandler, Handler mainHandler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.networkHelper = networkHelper;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onShopRemoved(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onShopRemoved();
            }
        });
    }

    private void onCantRemoveShop(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onCantRemoveShop();
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                carRepository.getCarsByUserId(user.getId())
                        .doOnNext(carListResponse -> {
                            Log.d(TAG,"getCarsByUserId() response: "+carListResponse);
                            List<Car> cars = carListResponse.getData();
                            if (cars == null){
                                RemoveShopUseCaseImpl.this.onError(RequestError.getUnknownError());
                                return;
                            }
                            for(Car c : cars){
                                if(c.getShopId() == dealership.getId()){
                                    RemoveShopUseCaseImpl.this.onCantRemoveShop();
                                    return;
                                }
                            }
                            shopRepository.delete(dealership.getId(), user.getId(), new Repository.Callback<Object>() {
                                @Override
                                public void onSuccess(Object response) {
                                    RemoveShopUseCaseImpl.this.onShopRemoved();
                                }

                                @Override
                                public void onError(RequestError error) {
                                    RemoveShopUseCaseImpl.this.onError(error);
                                }
                            });
                        }).onErrorReturn(err -> {
                            Log.d(TAG,"getCarsByUserId() err: "+err);
                            return new Response<List<Car>>(null,false);
                        }).subscribeOn(Schedulers.io())
                        .subscribe();
            }
            @Override
            public void onError(RequestError error) {
                RemoveShopUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(Dealership dealership, RemoveShopUseCase.Callback callback) {
        this.dealership = dealership;
        this.callback = callback;
        useCaseHandler.post(this);
    }

}

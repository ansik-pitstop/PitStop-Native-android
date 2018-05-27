package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Matthew on 2017-06-20.
 */

public class GetCarByCarIdUseCaseImpl implements GetCarByCarIdUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarRepository carRepository;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, UserRepository userRepository
            , ShopRepository shopRepository, Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.carRepository = carRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(int carId,Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: carId="+carId
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.carId = carId;
        useCaseHandler.post(this);
    }

    private void onCarGot(Car car, Dealership dealership){
        Logger.getInstance().logI(TAG,"Use case finished result: car="+car+", dealership="+dealership
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarGot(car, dealership));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    };

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {

            @Override
            public void onSuccess(User user) {
                carRepository.get(carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .subscribe(response -> {
                    Log.d(TAG,"carRepository.get() car: "+response.getData());
                    carRepository.getShopId(response.getData().getId(), new Repository.Callback<Integer>() {

                        @Override
                        public void onSuccess(Integer shopId) {
                            shopRepository.get(shopId, new Repository.Callback<Dealership>() {

                                @Override
                                public void onSuccess(Dealership dealership) {
                                    GetCarByCarIdUseCaseImpl.this.onCarGot(response.getData(), dealership);
                                }

                                @Override
                                public void onError(RequestError error) {
                                    GetCarByCarIdUseCaseImpl.this.onError(error);
                                }
                            });
                        }

                        @Override
                        public void onError(RequestError error) {
                            GetCarByCarIdUseCaseImpl.this.onError(error);
                        }
                    });
                }, err -> GetCarByCarIdUseCaseImpl.this.onError(new RequestError(err)));
            }
            @Override
            public void onError(RequestError error) {
                GetCarByCarIdUseCaseImpl.this.onError(error);
            }
        });
    }
}

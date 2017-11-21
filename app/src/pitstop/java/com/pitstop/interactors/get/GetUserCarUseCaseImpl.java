package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private CarRepository carRepository;
    private ShopRepository shopRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetUserCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , ShopRepository shopRepository, Handler useCaseHandler, Handler mainHandler) {

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.shopRepository = shopRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onCarRetrieved(Car car, Dealership dealership, boolean isLocal){
        Logger.getInstance().logI(TAG, "Use case finished: car="+car+", dealership="+dealership+", local="+isLocal
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarRetrieved(car, dealership, isLocal));
    }
    private void onNoCarSet(boolean isLocal){
        Logger.getInstance().logI(TAG, "Use case finished: no car set! local="+isLocal
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoCarSet(isLocal));
    }
    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> {
            if(error!=null)
                callback.onError(error);
        });
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings userSettings) {

                //Main car is stored in user settings, retrieve it from there
                if (userSettings.hasMainCar()){
                    carRepository.get(userSettings.getCarId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                            .doOnNext(response -> {
                        Log.d(TAG,"carRepository.get() car: "+response.getData());
                        if (response.getData() == null){
                            GetUserCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                            return;
                        }
                        response.getData().setCurrentCar(true);
                        shopRepository.get(response.getData().getShopId(), new Repository.Callback<Dealership>() {

                            @Override
                            public void onSuccess(Dealership dealership) {
                                GetUserCarUseCaseImpl.this.onCarRetrieved(response.getData(), dealership, response.isLocal());
                            }

                            @Override
                            public void onError(RequestError error) {
                                GetUserCarUseCaseImpl.this.onError(error);
                            }
                        });
                    }).doOnError(err -> {
                        Log.d(TAG,"doOnError() err: "+err);
                        GetUserCarUseCaseImpl.this.onError(new RequestError(err));
                    })
                    .onErrorReturn(err -> {
                        Log.d(TAG,"onErrorReturn() err: "+err);
                        return new RepositoryResponse<>(null,false);
                    })
                    .subscribe();
                    return;
                }

                /*User settings doesn't have mainCar stored, we cannot trust this because settings
                ** could potentially be corrupted, so perform a double-check by retrieving cars*/
                carRepository.getCarsByUserId(userSettings.getUserId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnError(err -> GetUserCarUseCaseImpl.this.onError(new RequestError(err)))
                        .doOnNext(carListResponse -> {
                            List<Car> carList = carListResponse.getData();
                            if (carList == null){
                                GetUserCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                            }
                            else if (carList.isEmpty()){
                                GetUserCarUseCaseImpl.this.onNoCarSet(carListResponse.isLocal());
                            }
                            else{
                                shopRepository.get(carList.get(0).getShopId(), new Repository.Callback<Dealership>() {
                                    @Override
                                    public void onSuccess(Dealership dealership) {
                                        GetUserCarUseCaseImpl.this.onCarRetrieved(carList.get(0)
                                                , dealership,carListResponse.isLocal());

                                    }

                                    @Override
                                    public void onError(RequestError error) {
                                        GetUserCarUseCaseImpl.this.onError(error);
                                    }
                                });
                                //Fix corrupted user settings
                                userRepository.setUserCar(userSettings.getUserId(), carList.get(0).getId()
                                        , new Repository.Callback<Object>() {
                                            @Override
                                            public void onSuccess(Object response){
                                                //Successfully fixed corrupted settings
                                            }
                                            @Override
                                            public void onError(RequestError error){
                                                //Error fixing corrupted settings
                                            }
                                        });
                            }
                        }).onErrorReturn(err -> {
                            Log.d(TAG,"getCarsByUserId() err: "+err);
                            return new RepositoryResponse<List<Car>>(null,true);
                        })
                        .subscribe();
            }

            @Override
            public void onError(RequestError error) {
                GetUserCarUseCaseImpl.this.onError(error);
            }
        });
    }
}

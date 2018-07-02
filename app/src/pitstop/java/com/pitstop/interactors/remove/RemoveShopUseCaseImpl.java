package com.pitstop.interactors.remove;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
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
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

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
        Logger.getInstance().logI(TAG,"Use case finished: shop removed"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onShopRemoved());
    }

    private void onCantRemoveShop(){
        Logger.getInstance().logI(TAG,"Use case finished: cant remove shop"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onCantRemoveShop());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Disposable disposable = carRepository.getCarsByUserId(user.getId(),Repository.DATABASE_TYPE.REMOTE)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnError(err -> RemoveShopUseCaseImpl.this.onError(new RequestError(err)))
                        .doOnNext(carListResponse -> {
                            if (carListResponse.isLocal()) return;
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
                            return new RepositoryResponse<List<Car>>(null,false);
                        })
                        .subscribe();
                compositeDisposable.add(disposable);
            }
            @Override
            public void onError(RequestError error) {
                RemoveShopUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(Dealership dealership, RemoveShopUseCase.Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: dealership="+dealership
                , DebugMessage.TYPE_USE_CASE);
        this.dealership = dealership;
        this.callback = callback;
        useCaseHandler.post(this);
    }

}

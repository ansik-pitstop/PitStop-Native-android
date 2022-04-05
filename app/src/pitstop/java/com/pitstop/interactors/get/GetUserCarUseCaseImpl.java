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
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
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
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Repository.DATABASE_TYPE requestType = Repository.DATABASE_TYPE.BOTH;
    private Integer carId;

    public GetUserCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , ShopRepository shopRepository, Handler useCaseHandler, Handler mainHandler) {

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.shopRepository = shopRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Integer carId, Repository.DATABASE_TYPE requestType, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.requestType = requestType;
        this.carId = carId;
        useCaseHandler.post(this);
    }

    private void onCarRetrieved(Car car, Dealership dealership, boolean isLocal){
        Logger.getInstance().logI(TAG, "Use case finished: car="+car+", dealership="+dealership+", local="+isLocal
                , DebugMessage.TYPE_USE_CASE);
        if (!isLocal && ( requestType == Repository.DATABASE_TYPE.BOTH || requestType == Repository.DATABASE_TYPE.REMOTE)){
            compositeDisposable.clear();
        }else if (isLocal && requestType == Repository.DATABASE_TYPE.LOCAL){
            compositeDisposable.clear();
        }
        mainHandler.post(() -> callback.onCarRetrieved(car, dealership, isLocal));
    }

    private void onNoCarSet(boolean isLocal){
        Logger.getInstance().logI(TAG, "Use case finished: no car set! local="+isLocal
                , DebugMessage.TYPE_USE_CASE);
        if (!isLocal && ( requestType == Repository.DATABASE_TYPE.BOTH || requestType == Repository.DATABASE_TYPE.REMOTE)){
            compositeDisposable.clear();
        }else if (isLocal && requestType == Repository.DATABASE_TYPE.LOCAL){
            compositeDisposable.clear();
        }
        mainHandler.post(() -> callback.onNoCarSet(isLocal));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> {
            if(error!=null)
                callback.onError(error);
        });
    }

    @Override
    public void run() {
        Disposable disposable = carRepository.get(this.carId, requestType)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(), true)
                .subscribe(response -> {
                    Log.d(TAG,"carRepository.get() isLocal?"+response.isLocal()+", car: "+response.getData());
                    if (response.getData() == null && !response.isLocal()){
                        GetUserCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                        return;
                    }else if (response.getData() == null && response.isLocal()){
                        return;
                    }
                    carRepository.setCurrent(response.getData());
                    GetUserCarUseCaseImpl.this.onCarRetrieved(response.getData()
                            , null, response.isLocal());
                }, err ->{
                    GetUserCarUseCaseImpl.this.onError(new RequestError(err));
                });
        compositeDisposable.add(disposable);
    }
}

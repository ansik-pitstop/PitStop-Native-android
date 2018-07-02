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

    public GetUserCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , ShopRepository shopRepository, Handler useCaseHandler, Handler mainHandler) {

        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.shopRepository = shopRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Repository.DATABASE_TYPE requestType, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.requestType = requestType;
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
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings userSettings) {

                //Main car is stored in user settings, retrieve it from there
                if (userSettings.hasMainCar()){
                    Disposable disposable = carRepository.get(userSettings.getCarId(),requestType)
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.computation(), true)
                            .subscribe(response -> {
                                Log.d(TAG,"carRepository.get() isLocal?"+response.isLocal()+", car: "+response.getData());
                                if (response.getData() == null && !response.isLocal()){
                                    GetUserCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                                    return;
                                }else if (response.getData() == null && response.isLocal()){
                                    return;
                                }
                                response.getData().setCurrentCar(true);

                                GetUserCarUseCaseImpl.this.onCarRetrieved(response.getData()
                                        , response.getData().getShop()
                                        , response.isLocal());

                            }, err ->{
                                GetUserCarUseCaseImpl.this.onError(new RequestError(err));
                            });
                    compositeDisposable.add(disposable);
                    return;
                }

                /*User settings doesn't have mainCar stored, we cannot trust this because settings
                ** could potentially be corrupted, so perform a double-check by retrieving cars*/
                Disposable disposable = carRepository.getCarsByUserId(userSettings.getUserId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation(),true)
                        .subscribe(carListResponse -> {
                            List<Car> carList = carListResponse.getData();
                            if (carList.isEmpty()){
                                GetUserCarUseCaseImpl.this.onNoCarSet(carListResponse.isLocal());
                            } else{
                                GetUserCarUseCaseImpl.this.onCarRetrieved(carList.get(0)
                                        , carList.get(0).getShop(),carListResponse.isLocal());
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
                        },err->{
                            GetUserCarUseCaseImpl.this.onError(new RequestError(err));
                        });
                compositeDisposable.add(disposable);
            }

            @Override
            public void onError(RequestError error) {
                GetUserCarUseCaseImpl.this.onError(error);
            }
        });
    }
}

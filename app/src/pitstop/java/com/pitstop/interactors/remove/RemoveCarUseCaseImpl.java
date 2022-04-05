package com.pitstop.interactors.remove;

import android.os.Handler;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.TripRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

/*
Removes the provided car, if the car was the users current car it will
set the users current car to the next most recent car
 */

public class RemoveCarUseCaseImpl implements RemoveCarUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarRepository carRepository;
    private UserRepository userRepository;
    private TripRepository tripRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Integer carId;
    private Integer userId;

    private EventSource eventSource;

    public RemoveCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , TripRepository tripRepository, CarIssueRepository carIssueRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler){
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.tripRepository = tripRepository;
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(Integer userId, Integer carId, String eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: carToDeleteId="+carId
                        +", eventSource="+eventSource
                , DebugMessage.TYPE_USE_CASE);
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carId = carId;
        this.userId = userId;
        useCaseHandler.post(this);
    }

    private void onCarRemoved(){
        Logger.getInstance().logI(TAG,"Use case execution finished: car removed"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onCarRemoved());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        carRepository.delete(userId, carId, new CarRepository.Callback<Object>() {
            @Override
            public void onSuccess(Object response) {
                carIssueRepository.deleteLocalCarIssueData(carId);
                userRepository.getCurrentUser(new Repository.Callback<User>() {

                    @Override
                    public void onSuccess(User user) {
                        onCarRemoved();
//                        Disposable disposable = carRepository.getCarsByUserId(user.getId(),Repository.DATABASE_TYPE.REMOTE)
//                                .subscribeOn(Schedulers.computation())
//                                .observeOn(Schedulers.io())
//                                .doOnError(err -> RemoveCarUseCaseImpl.this.onError(new RequestError(err)))
//                                .doOnNext(carListResponse -> {
//                                    if (carListResponse.isLocal()) return;
//                                    Log.d(TAG,"carRepository.getCarsByUserId() response: "+carListResponse);
//                                    List<Car> cars = carListResponse.getData();
//                                    if (cars == null){
//                                        RemoveCarUseCaseImpl.this.onError(RequestError.getUnknownError());
//                                    }
////                                    onCarRemoved();
//                                }).onErrorReturn(err -> {
//                                    Log.d(TAG,"carRepository.getCarsByUserId() err: "+err);
//                                    return new RepositoryResponse<List<Car>>(null,false);
//                                }).subscribe();
//                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void onError(RequestError error) {
                        RemoveCarUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                RemoveCarUseCaseImpl.this.onError(error);
            }
        });
    }
}

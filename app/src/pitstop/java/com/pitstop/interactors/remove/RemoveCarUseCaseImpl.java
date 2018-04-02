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
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.TripRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
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
    private int carToDeleteId;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;

    private EventSource eventSource;

    public RemoveCarUseCaseImpl(UserRepository userRepository, CarRepository carRepository, TripRepository tripRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler){
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.tripRepository = tripRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(int carToDeleteId,String eventSource, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: carToDeleteId="+carToDeleteId
                        +", eventSource="+eventSource
                , DebugMessage.TYPE_USE_CASE);
        this.eventSource = new EventSourceImpl(eventSource);
        this.carToDeleteId = carToDeleteId;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onCarRemoved(){
        Logger.getInstance().logI(TAG,"Use case execution finished: car removed"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarRemoved());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                final String[] carToDeleteVin = {""};

                // GET the VIN from the car to be deleted BEFORE it
                carRepository.get(carToDeleteId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnNext(carRepositoryResponse -> {

                            carToDeleteVin[0] = carRepositoryResponse.getData().getVin();

                        })
                        .subscribe();

                carRepository.delete(carToDeleteId, new CarRepository.Callback<Object>() {

                    @Override
                    public void onSuccess(Object response) {

                        // Once the car is successfully delete, let's delete all the associated Trips from the local DB
                        tripRepository.deleteAllTripsFromCarVin(carToDeleteVin[0], new Repository.Callback<Object>() {
                            @Override
                            public void onSuccess(Object data) {
                                Log.d(TAG, "tripRepository.deleteTripsFromCarVin() response: " + data);
                            }

                            @Override
                            public void onError(RequestError error) {
                                Log.d(TAG, "tripRepository.deleteTripsFromCarVin() error: " + error);
                                RemoveCarUseCaseImpl.this.onError(error);
                            }
                        });

                        if(data.getCarId() == carToDeleteId){// deleted the users current car
                            userRepository.getCurrentUser(new Repository.Callback<User>() {

                                @Override
                                public void onSuccess(User user) {
                                    carRepository.getCarsByUserId(user.getId())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                                            .doOnError(err -> RemoveCarUseCaseImpl.this.onError(new RequestError(err)))
                                            .doOnNext(carListResponse -> {
                                                if (carListResponse.isLocal()) return;
                                                Log.d(TAG,"carRepository.getCarsByUserId() response: "+carListResponse);
                                                List<Car> cars = carListResponse.getData();
                                                if (cars == null){
                                                    RemoveCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                                                    return;
                                                }

                                                int carId = cars.size() > 0 ? cars.get(cars.size() - 1).getId() : -1;
                                                userRepository.setUserCar(user.getId(), carId, new Repository.Callback<Object>() {

                                                    @Override
                                                    public void onSuccess(Object object) {
                                                        EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                                                ,eventSource));
                                                        RemoveCarUseCaseImpl.this.onCarRemoved();
                                                    }

                                                    @Override
                                                    public void onError(RequestError error) {
                                                        RemoveCarUseCaseImpl.this.onError(error);
                                                    }
                                                });
                                            }).onErrorReturn(err -> {
                                                Log.d(TAG,"carRepository.getCarsByUserId() err: "+err);
                                                return new RepositoryResponse<List<Car>>(null,false);
                                            })
                                            .subscribe();
                                }

                                @Override
                                public void onError(RequestError error) {
                                    RemoveCarUseCaseImpl.this.onError(error);
                                }
                            });
                        }else{
                            EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                            EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                    ,eventSource));
                            RemoveCarUseCaseImpl.this.onCarRemoved();
                        }
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

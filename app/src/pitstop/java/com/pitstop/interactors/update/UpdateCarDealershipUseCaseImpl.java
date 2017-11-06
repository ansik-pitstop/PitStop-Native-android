package com.pitstop.interactors.update;

import android.os.Handler;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by Matthew on 2017-06-20.
 */

public class UpdateCarDealershipUseCaseImpl implements UpdateCarDealershipUseCase {
    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private CarRepository carRepository;
    private UserRepository userRepository;
    private UpdateCarDealershipUseCase.Callback callback;

    private EventSource eventSource;

    private int carId;
    private Dealership dealership;

    public UpdateCarDealershipUseCaseImpl(CarRepository carRepository
            , UserRepository userRepository, Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onCarDealerUpdated(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onCarDealerUpdated();
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
    public void execute(int carId, Dealership dealership, String eventSource, UpdateCarDealershipUseCase.Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.carId = carId;
        this.dealership = dealership;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        Log.d(TAG,"run() dealership: "+dealership);
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG,"user: "+user);
                carRepository.get(carId).doOnNext(response -> {
                    Log.d(TAG,"carRepository.get() response: "+response.getData());
                    response.getData().setShopId(dealership.getId());
                    response.getData().setShop(dealership);
                    carRepository.update(response.getData(), new Repository.Callback<Object>() {
                        @Override
                        public void onSuccess(Object response) {
                            Log.d(TAG,"carRepository.update() ");
                            EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP);
                            EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                    ,eventSource));
                            UpdateCarDealershipUseCaseImpl.this.onCarDealerUpdated();
                        }

                        @Override
                        public void onError(RequestError error) {
                            UpdateCarDealershipUseCaseImpl.this.onError(error);
                        }
                    });
                }).onErrorReturn(err -> {
                    Log.d(TAG,"getCar error: "+err.getMessage());
                    return new RepositoryResponse<>(null,false);
                }).subscribeOn(Schedulers.io())
                .subscribe();
            }

            @Override
            public void onError(RequestError error) {
                UpdateCarDealershipUseCaseImpl.this.onError(error);

            }
        });
    }
}

package com.pitstop.interactors.update;

import android.os.Handler;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.android.schedulers.AndroidSchedulers;
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
        Logger.getInstance().logI(TAG, "Use case finished: car dealer updated"
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarDealerUpdated());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void execute(int carId, Dealership dealership, String eventSource, UpdateCarDealershipUseCase.Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started: carId="+carId+", dealership="+dealership
                , false, DebugMessage.TYPE_USE_CASE);
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
                carRepository.get(carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnError(err -> UpdateCarDealershipUseCaseImpl.this.onError(new RequestError(err)))
                        .doOnNext(response -> {
                    if (response.isLocal()) return;

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
                })
                .subscribe();
            }

            @Override
            public void onError(RequestError error) {
                UpdateCarDealershipUseCaseImpl.this.onError(error);

            }
        });
    }
}

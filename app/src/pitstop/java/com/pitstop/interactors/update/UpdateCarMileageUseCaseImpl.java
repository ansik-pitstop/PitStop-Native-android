package com.pitstop.interactors.update;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.observer.MileageObservable;
import com.pitstop.observer.MileageObserver;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.jetbrains.annotations.NotNull;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class UpdateCarMileageUseCaseImpl implements UpdateCarMileageUseCase, MileageObserver{

    private final String TAG = getClass().getSimpleName();

    private CarRepository carRepository;
    private UserRepository userRepository;
    private Handler usecaseHandler;
    private Handler mainHandler;


    private Callback callback;

    private double mileage;

    public UpdateCarMileageUseCaseImpl(CarRepository carRepository, UserRepository userRepository
            , Handler usecaseHandler, Handler mainHandler) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.usecaseHandler = usecaseHandler;
        this.mainHandler = mainHandler;

    }

    @Override
    public void execute(MileageObservable mileageObservable, double mileage, Callback callback){
        Logger.getInstance().logI(TAG, "Use case execution started: mileage="+mileage
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.mileage = mileage;
        usecaseHandler.post(this);
        mileageObservable.subscribe(this);
    }



    private void onMileageUpdated(){
        Logger.getInstance().logI(TAG, "Use case finished: mileage updated"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onMileageUpdated());
    }

    private void onNoCarAdded(){
        Logger.getInstance().logI(TAG, "Use case finished: no car added"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        Log.d(TAG,"run()");
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings settings) {
                Log.d(TAG,"got current user settings: "+settings);
                if (!settings.hasMainCar()){
                    UpdateCarMileageUseCaseImpl.this.onNoCarAdded();
                    return;
                }

                carRepository.get(settings.getCarId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(usecaseHandler.getLooper()))
                        .doOnError(err -> UpdateCarMileageUseCaseImpl.this.onError(new RequestError(err)))
                        .doOnNext(response -> {
                            if (response.isLocal()) return;
                    Log.d(TAG,"carRepository.get() response: "+response);
                    if (response.getData() == null){
                        callback.onError(RequestError.getUnknownError());
                        return;
                    }
                    response.getData().setTotalMileage(mileage);
                    carRepository.update(response.getData(), new Repository.Callback<Object>() {

                        @Override
                        public void onSuccess(Object response){
                            Log.d(TAG,"carRepository.update() response: "+response);
                            UpdateCarMileageUseCaseImpl.this.onMileageUpdated();
                        }

                        @Override
                        public void onError(RequestError error){
                            UpdateCarMileageUseCaseImpl.this.onError(error);
                        }
                    });
                }).onErrorReturn(err -> {
                    Log.d(TAG,"carRepository.get() error: "+err);
                    return new RepositoryResponse<>(null,false);
                    //Todo: error handling
                }).subscribe();
            }

            @Override
            public void onError(RequestError error) {
                UpdateCarMileageUseCaseImpl.this.onError(error);
            }
        });
    }


    @Override
    public void onMileageAndRtcGot(@NotNull String mileage, @NotNull String rtc) {

    }

    @Override
    public void onGetMileageAndRtcError() {

    }

    @Override
    public void onNotConnected() {

    }
}

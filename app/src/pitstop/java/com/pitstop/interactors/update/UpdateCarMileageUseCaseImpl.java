package com.pitstop.interactors.update;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class UpdateCarMileageUseCaseImpl implements UpdateCarMileageUseCase {

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
    public void execute(double mileage, Callback callback){
        this.callback = callback;
        this.mileage = mileage;
        usecaseHandler.post(this);
    }

    private void onMileageUpdated(){
        mainHandler.post(() -> callback.onMileageUpdated());
    }

    private void onNoCarAdded(){
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
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

                carRepository.get(settings.getCarId()).doOnNext(response -> {
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
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();
            }

            @Override
            public void onError(RequestError error) {
                UpdateCarMileageUseCaseImpl.this.onError(error);
            }
        });
    }
}

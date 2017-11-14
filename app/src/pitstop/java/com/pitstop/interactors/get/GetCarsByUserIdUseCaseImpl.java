package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Matt on 2017-06-13.
 */

public class GetCarsByUserIdUseCaseImpl implements GetCarsByUserIdUseCase {

    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private CarRepository carRepository;
    private UserRepository userRepository;

    private GetCarsByUserIdUseCase.Callback callback;


    public GetCarsByUserIdUseCaseImpl(UserRepository userRepository,CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.carRepository = carRepository;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(GetCarsByUserIdUseCase.Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onCarsRetrieved(List<Car> cars){
        mainHandler.post(() -> callback.onCarsRetrieved(cars));
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()) {
                    GetCarsByUserIdUseCaseImpl.this.onCarsRetrieved(new ArrayList<Car>());
                    return;
                }

                userRepository.getCurrentUser(new Repository.Callback<User>(){
                    @Override
                    public void onSuccess(User user) {
                        carRepository.getCarsByUserId(user.getId())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                                .doOnNext(carListResponse -> {
                                    if (carListResponse.getData() == null){
                                        GetCarsByUserIdUseCaseImpl.this.onError(RequestError.getUnknownError());
                                    }else{
                                        GetCarsByUserIdUseCaseImpl.this.onCarsRetrieved(carListResponse.getData());
                                    }
                                }).doOnError(err -> GetCarsByUserIdUseCaseImpl.this.onError(new RequestError(err)))
                                .onErrorReturn(err -> {
                                    Log.d(TAG,"carRepository.getCarsByUserId() err: "+err);
                                    return new RepositoryResponse<List<Car>>(null,false);
                                }).subscribe();
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetCarsByUserIdUseCaseImpl.this.onError(error);
                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                GetCarsByUserIdUseCaseImpl.this.onError(error);
            }
        });

    }
}

package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.text.SimpleDateFormat;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Matthew on 2017-07-17.
 */

public class RequestServiceUseCaseImpl implements RequestServiceUseCase {

    private final String TAG = getClass().getSimpleName();

    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private String state;
    private String timeStamp;
    private String comments;
    private Callback callback;

    public RequestServiceUseCaseImpl(CarIssueRepository carIssueRepository
            , UserRepository userRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onServicesRequested(){
        Logger.getInstance().logI(TAG,"Use case finished: service requested"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onServicesRequested());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
                    @Override
                    public void onSuccess(Settings data) {
                        carRepository.get(data.getCarId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                            .doOnError(err -> RequestServiceUseCaseImpl.this.onError(new RequestError(err)))
                            .doOnNext(response -> {
                                if (response.isLocal()) return;
                                if (response.getData() == null){
                                    callback.onError(RequestError.getUnknownError());
                                    return;
                                }
                                Car car = response.getData();

                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Appointment appointment = new Appointment(car.getShopId()
                                        , state, simpleDateFormat.parse(timeStamp), comments);
                                carIssueRepository.requestService(user.getId(), car.getId(), appointment
                                        , new Repository.Callback<Object>() {

                                            @Override
                                            public void onSuccess(Object object) {
                                                RequestServiceUseCaseImpl.this.onServicesRequested();
                                            }

                                            @Override
                                            public void onError(RequestError error){
                                                RequestServiceUseCaseImpl.this.onError(error);
                                            }
                                        });
                            }).onErrorReturn(err -> new RepositoryResponse<>(null,false))
                            .subscribe();
                    }

                    @Override
                    public void onError(RequestError error) {
                        RequestServiceUseCaseImpl.this.onError(error);
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                RequestServiceUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(String state, String timeStamp, String comments, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: state"+state
                        +", timeStamp: "+timeStamp+", comments: "+comments
                , DebugMessage.TYPE_USE_CASE);
        this.state = state;
        this.timeStamp = timeStamp;
        this.comments = comments;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

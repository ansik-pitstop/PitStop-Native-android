package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.Date;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
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
    private Date date;
    private String comments;
    private Callback callback;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Integer carId;

    public RequestServiceUseCaseImpl(CarIssueRepository carIssueRepository
            , UserRepository userRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.carId = carId;
    }

    private void onServicesRequested(){
        Logger.getInstance().logI(TAG,"Use case finished: service requested"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onServicesRequested());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        Log.d(TAG,"run()");
        userRepository.getCurrentUser(new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG,"got user: "+user);
                Disposable disposable = carRepository.get(carId, Repository.DATABASE_TYPE.REMOTE)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe(response -> {
                                    Log.d(TAG, "got car: " + response);
                                    Log.d(TAG, "remote data, proceeding");
                                    if (response.getData() == null) {
                                        Log.d(TAG, "data null, returning");
                                        callback.onError(RequestError.getUnknownError());
                                        return;
                                    }
                                    Log.d(TAG, "data is fine, proceeding");
                                    Car car = response.getData();

                                    Appointment appointment = new Appointment(car.getShopId()
                                            , state, date, comments);
                                    Log.d(TAG, "requestingService");
                                    carIssueRepository.requestService(user.getId(), car.getId(), appointment
                                            , new Repository.Callback<Object>() {

                                                @Override
                                                public void onSuccess(Object object) {
                                                    RequestServiceUseCaseImpl.this.onServicesRequested();
                                                }

                                                @Override
                                                public void onError(RequestError error) {
                                                    RequestServiceUseCaseImpl.this.onError(error);
                                                }
                                            });
                                }, err -> RequestServiceUseCaseImpl.this.onError(new RequestError(err))
                        );
                compositeDisposable.add(disposable);
            }
            @Override
            public void onError(RequestError error) {
                RequestServiceUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(String state, Date date, String comments, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: state"+state
                        +", date: "+date+", comments: "+comments
                , DebugMessage.TYPE_USE_CASE);
        this.state = state;
        this.date = date;
        this.comments = comments;
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetDoneServicesUseCaseImpl implements GetDoneServicesUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private CarRepository carRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public GetDoneServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotDoneServices(List<CarIssue> doneServices){
        Logger.getInstance().logI(TAG, "Use case finished: doneServices="+doneServices
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onGotDoneServices(doneServices));
    }

    private void onNoCarAdded(){
        Logger.getInstance().logI(TAG, "Use case finished: no car added!"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()) {
                    //Double check car repo in case settings is out of sync
                    Disposable disposable = carRepository.getCarsByUserId(data.getUserId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()),true)
                            .subscribe(next -> {
                                //Ignore local responses
                                if (next.isLocal()){}
                                else if (next.getData() != null && next.getData().size() > 0){
                                    getDoneCarIssues(next.getData().get(0).getId());

                                    //Fix settings
                                    userRepository.setUserCar(data.getUserId(), next.getData().get(0).getId()
                                            , new Repository.Callback<Object>() {

                                                @Override
                                                public void onSuccess(Object data) {
                                                    Log.d(TAG,"fixed settings");
                                                }

                                                @Override
                                                public void onError(RequestError error) {
                                                    Log.d(TAG,"Error fixing settings");
                                                }
                                            });
                                }else{
                                    GetDoneServicesUseCaseImpl.this.onNoCarAdded();
                                }
                            },error -> {
                                GetDoneServicesUseCaseImpl.this
                                        .onError(new RequestError(error));
                            });
                    compositeDisposable.add(disposable);
                } else getDoneCarIssues(data.getCarId());
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getCurrentUserSettings.onError() err: "+error);
                GetDoneServicesUseCaseImpl.this.onError(error);
            }
        });
    }

    private void getDoneCarIssues(int carId){
        //Use the current users car to get all the current issues
        carIssueRepository.getDoneCarIssues(carId
                , new CarIssueRepository.Callback<List<CarIssue>>() {

                    @Override
                    public void onSuccess(List<CarIssue> carIssueDone) {
                        GetDoneServicesUseCaseImpl.this.onGotDoneServices(carIssueDone);
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"getDoneCarIssues.onError() err: "+error);
                        GetDoneServicesUseCaseImpl.this.onError(error);
                    }
                });
    }
}

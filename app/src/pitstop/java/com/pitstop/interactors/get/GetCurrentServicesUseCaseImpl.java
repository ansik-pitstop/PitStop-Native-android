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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetCurrentServicesUseCaseImpl implements GetCurrentServicesUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private CarRepository carRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public GetCurrentServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository
            , CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {

        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.carRepository = carRepository;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues, boolean local){
        Logger.getInstance().logI(TAG, "Use case finished: currentServices="+currentServices+", customIssues="+customIssues
                , DebugMessage.TYPE_USE_CASE);
        if (!local) compositeDisposable.clear();
        mainHandler.post(() -> callback.onGotCurrentServices(currentServices,customIssues,local));
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

                if (!data.hasMainCar()){
                    //Check user car list
                    Disposable disposable = carRepository.getCarsByUserId(data.getUserId(),Repository.DATABASE_TYPE.REMOTE)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.io(),true)
                            .subscribe(next -> {
                                //Ignore local responses
                                if (next.isLocal()){}
                                else if (next.getData() != null && next.getData().size() > 0){
                                    getCurrentCarIssues(next.getData().get(0).getId());

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
                                    GetCurrentServicesUseCaseImpl.this.onNoCarAdded();
                                }
                            },error -> {
                                GetCurrentServicesUseCaseImpl.this.onError(new RequestError(error));
                            });
                    compositeDisposable.add(disposable);
                } else getCurrentCarIssues(data.getCarId());
            }

            @Override
            public void onError(RequestError error) {
                GetCurrentServicesUseCaseImpl.this.onError(error);
            }
        });
    }

    private void getCurrentCarIssues(int carId){
        Disposable disposable = carIssueRepository.getCurrentCarIssues(carId, Repository.DATABASE_TYPE.REMOTE)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(), true)
                .subscribe(next -> {
                    if (next.getData() == null && !next.isLocal()){
                        RequestError err = RequestError.getUnknownError();
                        err.setMessage("Null data returned from car issue repository");
                        err.setError("Null data");
                        err.setStatusCode(0);
                        GetCurrentServicesUseCaseImpl.this.onError(err);
                        return;
                    }
                    List<CarIssue> preset = new ArrayList<CarIssue>();
                    List<CarIssue> custom = new ArrayList<CarIssue>();
                    for( CarIssue c: next.getData()){
                        if(c.getIssueType().equals(CarIssue.SERVICE_PRESET)){
                            preset.add(c);
                        }else if(c.getIssueType().equals(CarIssue.SERVICE_USER)){
                            custom.add(c);
                        }else{
                            preset.add(c);
                        }
                    }
                    GetCurrentServicesUseCaseImpl.this.onGotCurrentServices(preset,custom, next.isLocal());
                }, error -> {
                    error.printStackTrace();
                    Log.d(TAG,"error: "+error);
                    GetCurrentServicesUseCaseImpl.this.onError(new RequestError(error));

                });
        compositeDisposable.add(disposable);
    }
}

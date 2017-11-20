package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetCurrentServicesUseCaseImpl implements GetCurrentServicesUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetCurrentServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler useCaseHandler, Handler mainHandler) {

        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logE(TAG, "Use case execution started"
                , false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues){
        Logger.getInstance().logE(TAG, "Use case finished: currentServices="+currentServices+", customIssues="+customIssues
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onGotCurrentServices(currentServices,customIssues));
    }

    private void onNoCarAdded(){
        Logger.getInstance().logE(TAG, "Use case finished: no car added!"
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()){
                    GetCurrentServicesUseCaseImpl.this.onNoCarAdded();
                    return;
                }

                carIssueRepository.getCurrentCarIssues(data.getCarId(), new CarIssueRepository.Callback<List<CarIssue>>() {
                    @Override
                    public void onSuccess(List<CarIssue> carIssueCurrent) {
                        List<CarIssue> preset = new ArrayList<CarIssue>();
                        List<CarIssue> custom = new ArrayList<CarIssue>();
                        for( CarIssue c: carIssueCurrent){
                            if(c.getIssueType().equals(CarIssue.SERVICE_PRESET)){
                                preset.add(c);
                            }else if(c.getIssueType().equals(CarIssue.SERVICE_USER)){
                                custom.add(c);
                            }else{
                                preset.add(c);
                            }
                        }
                        GetCurrentServicesUseCaseImpl.this.onGotCurrentServices(preset,custom);
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetCurrentServicesUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                GetCurrentServicesUseCaseImpl.this.onError(error);
            }
        });
    }
}

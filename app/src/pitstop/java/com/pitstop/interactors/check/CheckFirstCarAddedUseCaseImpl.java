package com.pitstop.interactors.check;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

/**
 *
 * Created by Karol Zdebel on 6/8/2017.
 */

public class CheckFirstCarAddedUseCaseImpl implements CheckFirstCarAddedUseCase {

    private final String TAG =getClass().getSimpleName();

    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;


    public CheckFirstCarAddedUseCaseImpl(UserRepository userRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started", DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onFirstCarAddedChecked(boolean added){
        Logger.getInstance().logI(TAG,"Use case finished: added="+added, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onFirstCarAddedChecked(added));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        CheckFirstCarAddedUseCaseImpl.this.onFirstCarAddedChecked(true);
//        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
//            @Override
//            public void onSuccess(Settings userSettings) {
//
//                //If everything went right during the add car process, this should return true
//                if (userSettings.isFirstCarAdded()){
//
//                }else{
//                    CheckFirstCarAddedUseCaseImpl.this.onFirstCarAddedChecked(false);
//
//                }
//            }
//
//            @Override
//            public void onError(RequestError error) {
//                CheckFirstCarAddedUseCaseImpl.this.onError(error);
//            }
//        });
    }
}

package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 * Created by ishan on 2017-09-26.
 */

public class AddLicensePlateUseCaseImpl implements AddLicensePlateUseCase {

    private AddLicensePlateUseCase.Callback callback;
    private Handler mainHandler;
    private Handler useCaseHandler;
    private LocalSpecsStorage localSpecsStorage;



    public AddLicensePlateUseCaseImpl(UserRepository userRepository, Handler mainHandler, Handler useCasehandler,
                                      LocalSpecsStorage localSpecsStorage){
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCasehandler;
        this.localSpecsStorage = localSpecsStorage;
    }
    @Override
    public void execute(AddLicensePlateUseCase.Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    public void onLicensePlateStored(String licensePlate){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onLicensePlateStored(licensePlate);
            }
        });
    }


    @Override
    public void run() {





    }
}

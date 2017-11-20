package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.utils.Logger;

/**
 * Created by ishan on 2017-09-27.
 */

public class GetLicensePlateUseCaseImpl implements GetLicensePlateUseCase{

    private final String TAG = getClass().getSimpleName();

    private GetLicensePlateUseCase.Callback callback;
    private Handler mainHandler;
    private Handler useCaseHandler;
    private LocalSpecsStorage localSpecsStorage;

    private int carID;


    public GetLicensePlateUseCaseImpl(Handler mainHandler, Handler useCasehandler,
                                      LocalSpecsStorage localSpecsStorage){
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCasehandler;
        this.localSpecsStorage = localSpecsStorage;
    }

    @Override
    public void execute(int Carid, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution: carId="+Carid
                , false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.carID = Carid;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    public void onLicensePlateStored(String licensePlate){
        Logger.getInstance().logI(TAG, "Use case finished: license{late="+licensePlate
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onLicensePlateGot(licensePlate));
    }

    @Override
    public void run() {
        localSpecsStorage.getLicensePlate(carID, new Repository.Callback<String>() {
            @Override
            public void onSuccess(String data) {
                GetLicensePlateUseCaseImpl.this.onLicensePlateStored(data);
            }

            @Override
            public void onError(RequestError error) {
                GetLicensePlateUseCaseImpl.this.onError(error);
            }
        });

    }
}

package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.utils.Logger;

/**
 * Created by ishan on 2017-09-26.
 */

public class AddLicensePlateUseCaseImpl implements AddLicensePlateUseCase {

    private final String TAG = getClass().getSimpleName();

    private AddLicensePlateUseCase.Callback callback;
    private Handler mainHandler;
    private Handler useCaseHandler;
    private LocalSpecsStorage localSpecsStorage;
    private int carID;
    private String carLicensePlate;

    public AddLicensePlateUseCaseImpl(Handler mainHandler, Handler useCasehandler,
                                      LocalSpecsStorage localSpecsStorage){
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCasehandler;
        this.localSpecsStorage = localSpecsStorage;
    }
    @Override
    public void execute(int carid, String plate, AddLicensePlateUseCase.Callback callback) {
        Logger.getInstance().logE(TAG,"Use case execution started input: carId="+carid+", plate="+plate
                ,false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.carID = carid;
        this.carLicensePlate = plate;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    public void onLicensePlateStored(String licensePlate){
        Logger.getInstance().logE(TAG,"Use case finished: license plate= "+licensePlate
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onLicensePlateStored(licensePlate));
    }

    @Override
    public void run() {
        localSpecsStorage.deleteRecord(carID);
        localSpecsStorage.storeLicensePlate(carID, carLicensePlate, new Repository.Callback<String>() {
            @Override
            public void onSuccess(String data) {
                AddLicensePlateUseCaseImpl.this.onLicensePlateStored(data);
            }
            @Override
            public void onError(RequestError error) {
                AddLicensePlateUseCaseImpl.this.onError(error);
            }
        });
    }
}
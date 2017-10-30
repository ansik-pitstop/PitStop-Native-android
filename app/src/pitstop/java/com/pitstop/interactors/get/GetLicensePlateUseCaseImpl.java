package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by ishan on 2017-09-27.
 */

public class GetLicensePlateUseCaseImpl implements GetLicensePlateUseCase{

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
        this.callback = callback;
        this.carID = Carid;
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
                callback.onLicensePlateGot(licensePlate);
            }
        });
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

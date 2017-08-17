package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

public class HandlePidDataUseCaseImpl implements HandlePidDataUseCase {

    private PidRepository pidRepository;
    private Handler handler;
    private PidPackage pidPackage;
    private Callback callback;

    public HandlePidDataUseCaseImpl(PidRepository pidRepository, Handler handler) {
        this.pidRepository = pidRepository;
        this.handler = handler;
    }

    @Override
    public void execute(PidPackage pidPackage, Callback callback) {
        this.callback = callback;
        this.pidPackage = pidPackage;
        handler.post(this);
    }

    @Override
    public void run() {
        pidRepository.insertPid(pidPackage, new Repository.Callback<Object>() {
            @Override
            public void onSuccess(Object response){
                callback.onSuccess();
            }

            @Override
            public void onError(RequestError error){
                callback.onError(error);
            }
        });
    }
}

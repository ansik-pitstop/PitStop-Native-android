package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Matthew on 2017-06-19.
 */

public class GetPitstopShopsUseCaseImpl implements GetPitstopShopsUseCase {

    private final String TAG = getClass().getSimpleName();

    private ShopRepository shopRepository;
    private NetworkHelper networkHelper;
    private GetPitstopShopsUseCase.Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetPitstopShopsUseCaseImpl(ShopRepository shopRepository, NetworkHelper networkHelper
            , Handler useCaseHandler, Handler mainHandler){
        this.shopRepository = shopRepository;
        this.networkHelper = networkHelper;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onShopsGot(List<Dealership> dealerships){
        Logger.getInstance().logI(TAG, "Use case finished executing: dealerships="+dealerships
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onShopsGot(dealerships));
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        shopRepository.getPitstopShops(new Repository.Callback<List<Dealership>>() {
            @Override
            public void onSuccess(List<Dealership> dealershipList) {
                GetPitstopShopsUseCaseImpl.this.onShopsGot(dealershipList);
            }

            @Override
            public void onError(RequestError error) {
                GetPitstopShopsUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(GetPitstopShopsUseCase.Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

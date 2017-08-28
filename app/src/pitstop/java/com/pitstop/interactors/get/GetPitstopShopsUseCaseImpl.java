package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Matthew on 2017-06-19.
 */

public class GetPitstopShopsUseCaseImpl implements GetPitstopShopsUseCase {
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onShopsGot(dealerships);
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
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
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

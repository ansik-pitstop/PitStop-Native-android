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
    private Handler handler;

    public GetPitstopShopsUseCaseImpl(ShopRepository shopRepository, NetworkHelper networkHelper, Handler handler){
        this.shopRepository = shopRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void run() {
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        shopRepository.getPitstopShops(new Repository.Callback<List<Dealership>>() {
            @Override
            public void onSuccess(List<Dealership> dealershipList) {
                callback.onShopsGot(dealershipList);
            }

            @Override
            public void onError(RequestError error) {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(GetPitstopShopsUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }
}

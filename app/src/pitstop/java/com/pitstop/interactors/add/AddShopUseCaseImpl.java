package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

/**
 * Created by Matthew on 2017-06-21.
 */

public class AddShopUseCaseImpl implements AddShopUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private AddShopUseCase.Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Dealership dealership;

    public AddShopUseCaseImpl(ShopRepository shopRepository,UserRepository userRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    private void onShopAdded(){
        Logger.getInstance().logI(TAG,"Use case finished result: shop added successfully"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onShopAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void execute(Dealership dealership, AddShopUseCase.Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: dealership="+dealership
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.dealership = dealership;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                shopRepository.insert(dealership, user.getId(), new ShopRepository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object response) {
                        AddShopUseCaseImpl.this.onShopAdded();
                    }

                    @Override
                    public void onError(RequestError error) {
                        AddShopUseCaseImpl.this.onError(error);

                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                AddShopUseCaseImpl.this.onError(error);
            }
        });


    }
}

package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Matthew on 2017-06-26.
 */

public class GetUserShopsUseCaseImpl implements GetUserShopsUseCase {
    private NetworkHelper networkHelper;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private GetUserShopsUseCase.Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetUserShopsUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onShopGot(List<Dealership> dealerships){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onShopGot(dealerships);
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
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                shopRepository.getShopsByUserId(user.getId(), new Repository.Callback<List<Dealership>>() {
                    @Override
                    public void onSuccess(List<Dealership> dealerships) {
                        GetUserShopsUseCaseImpl.this.onShopGot(dealerships);
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetUserShopsUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                GetUserShopsUseCaseImpl.this.onError(error);
            }
        });
    }

    @Override
    public void execute(GetUserShopsUseCase.Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }
}

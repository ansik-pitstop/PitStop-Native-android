package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.User;
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
    private Handler handler;

    public GetUserShopsUseCaseImpl(ShopRepository shopRepository,UserRepository userRepository, NetworkHelper networkHelper, Handler handler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void run() {
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                shopRepository.getShopsByUserId(user.getId(), new Repository.Callback<List<Dealership>>() {
                    @Override
                    public void onSuccess(List<Dealership> dealerships) {
                        callback.onShopGot(dealerships);
                    }

                    @Override
                    public void onError(int error) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });
    }

    @Override
    public void execute(GetUserShopsUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }
}

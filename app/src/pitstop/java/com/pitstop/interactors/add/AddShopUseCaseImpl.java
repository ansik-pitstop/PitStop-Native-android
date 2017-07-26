package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matthew on 2017-06-21.
 */

public class AddShopUseCaseImpl implements AddShopUseCase {
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private AddShopUseCase.Callback callback;
    private Handler handler;
    private Dealership dealership;

    public AddShopUseCaseImpl(ShopRepository shopRepository,UserRepository userRepository, Handler handler) {
        this.handler = handler;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }


    @Override
    public void execute(Dealership dealership, AddShopUseCase.Callback callback) {
        this.callback = callback;
        this.dealership = dealership;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                shopRepository.insert(dealership, user.getId(), new ShopRepository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object response) {
                        callback.onShopAdded();
                    }

                    @Override
                    public void onError(RequestError error) {
                        callback.onError(error);

                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });


    }
}

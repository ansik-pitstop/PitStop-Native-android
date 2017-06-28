package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 * Created by Matthew on 2017-06-27.
 */

public class UpdateShopUseCaseImpl implements UpdateShopUseCase {
    private Handler handler;
    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private UpdateShopUseCase.Callback callback;

    private Dealership dealership;


    public UpdateShopUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository, CarRepository carRepository, Handler handler) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Dealership dealership, UpdateShopUseCase.Callback callback) {
        this.callback = callback;
        this.dealership = dealership;
        handler.post(this);
    }

    @Override
    public void run() {
      userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
          @Override
          public void onGotUser(User user) {
              shopRepository.update(dealership, user.getId(), new ShopRepository.ShopUpdateCallback() {
                  @Override
                  public void onShopUpdated() {
                      callback.onShopUpdated();
                  }

                  @Override
                  public void onError() {
                      callback.onError();
                  }
              });
          }
          @Override
          public void onError() {
              callback.onError();
          }
      });
    }
}

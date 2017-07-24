package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matthew on 2017-06-27.
 */

public class UpdateShopUseCaseImpl implements UpdateShopUseCase {
    private Handler handler;
    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private UpdateShopUseCase.Callback callback;

    private EventSource eventSource;

    private Dealership dealership;


    public UpdateShopUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository, CarRepository carRepository, Handler handler) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Dealership dealership,String eventSource, UpdateShopUseCase.Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.dealership = dealership;
        handler.post(this);
    }

    @Override
    public void run() {
      userRepository.getCurrentUser(new Repository.Callback<User>() {
          @Override
          public void onSuccess(User user) {
              shopRepository.update(dealership, user.getId(), new Repository.Callback<Object>() {
                  @Override
                  public void onSuccess(Object response) {
                      EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP);
                      EventBus.getDefault().post(new CarDataChangedEvent(eventType
                              ,eventSource));
                      callback.onShopUpdated();
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
}

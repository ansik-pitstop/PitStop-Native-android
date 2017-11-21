package com.pitstop.interactors.update;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matthew on 2017-06-27.
 */

public class UpdateShopUseCaseImpl implements UpdateShopUseCase {

    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private UpdateShopUseCase.Callback callback;

    private EventSource eventSource;
    private Dealership dealership;

    public UpdateShopUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository
            , CarRepository carRepository, Handler useCaseHandler, Handler mainHandler) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Dealership dealership,String eventSource, UpdateShopUseCase.Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE);
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.dealership = dealership;
        useCaseHandler.post(this);
    }

    private void onShopUpdated(){
        Logger.getInstance().logI(TAG, "Use case finished: shop updated"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onShopUpdated());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
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
                      UpdateShopUseCaseImpl.this.onShopUpdated();
                  }

                  @Override
                  public void onError(RequestError error) {
                      UpdateShopUseCaseImpl.this.onError(error);
                  }
              });
          }
          @Override
          public void onError(RequestError error) {
              UpdateShopUseCaseImpl.this.onError(error);
          }
      });
    }
}

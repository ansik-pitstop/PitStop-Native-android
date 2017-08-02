package com.pitstop.interactors.add;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matt on 2017-07-27.
 */

public class AddCarNoShopUseCaseImpl implements AddCarNoShopUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private ScannerRepository scannerRepository;
    private Handler handler;
    private Callback callback;

    private EventSource eventSource;

    private Car pendingCar;
    private String scannerName;


    public AddCarNoShopUseCaseImpl(CarRepository carRepository, ScannerRepository scannerRepository, UserRepository userRepository, Handler handler){
        this.carRepository = carRepository;
        this.scannerRepository = scannerRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Car pendingCar,String scannerName, String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.pendingCar = pendingCar;
        this.scannerName = scannerName;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                    carRepository.insertNoShop(user.getId(), (int)pendingCar.getBaseMileage(), pendingCar.getVin(), pendingCar.getScannerId(), new Repository.Callback<Car>() {
                        @Override
                        public void onSuccess(Car car) {
                            userRepository.setUserCar(user.getId(), car.getId(), new Repository.Callback<Object>() {
                                @Override
                                public void onSuccess(Object data) {
                                    ObdScanner scanner = new ObdScanner();
                                    if (pendingCar.getScannerId() == null || pendingCar.getScannerId().isEmpty() || scannerName == null){//empty scanner
                                        scanner.setCarId(car.getId());
                                        scanner.setScannerId("");
                                        scanner.setDeviceName("");
                                        scanner.setDatanum("");
                                    }else{//not empty
                                        scanner = new ObdScanner(car.getId(),pendingCar.getScannerId(),scannerName);
                                    }
                                    scannerRepository.createScanner(scanner, new Repository.Callback() {
                                        @Override
                                        public void onSuccess(Object data) {
                                            if(car.getShopId() == 0){
                                                callback.onCarAdded(car);
                                            }else {
                                                callback.onCarAddedWithBackendShop(car);
                                            }
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
                        @Override
                        public void onError(RequestError error) {
                            callback.onError(error);
                        }});
            }
            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}

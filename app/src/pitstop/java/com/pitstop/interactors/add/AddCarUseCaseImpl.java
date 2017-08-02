package com.pitstop.interactors.add;

import android.os.Handler;

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

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matt on 2017-07-27.
 */

public class AddCarUseCaseImpl implements AddCarUseCase {

    private CarRepository carRepository;
    private UserRepository userRepository;
    private ScannerRepository scannerRepository;
    private Handler handler;
    private Callback callback;

    private EventSource eventSource;

    private boolean carHasShop;
    private String vin;
    private String scannerId;
    private String scannerName;
    private double baseMileage;
    private int userId;
    private int shopId;


    public AddCarUseCaseImpl(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, Handler handler){
        this.carRepository = carRepository;
        this.scannerRepository = scannerRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String vin, double baseMileage, int userId, String scannerId, int shopId
            ,String scannerName, String eventSource, boolean carHasShop, Callback callback) {
        this.vin = vin;
        this.baseMileage = baseMileage;
        this.userId = userId;
        this.scannerId = scannerId;
        this.shopId = shopId;
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.scannerName = scannerName;
        this.carHasShop = carHasShop;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                    carRepository.insert(vin, baseMileage, userId, scannerId, shopId, carHasShop
                            , new Repository.Callback<Car>() {
                        @Override
                        public void onSuccess(Car car) {
                            userRepository.setUserCar(user.getId(), car.getId(), new Repository.Callback<Object>() {
                                @Override
                                public void onSuccess(Object data) {
                                    ObdScanner scanner = new ObdScanner();
                                    if (scannerId == null || scannerId.isEmpty()
                                            || scannerName == null){//empty scanner
                                        scanner.setCarId(car.getId());
                                        scanner.setScannerId("");
                                        scanner.setDeviceName("");
                                        scanner.setDatanum("");
                                    }else{//not empty
                                        scanner = new ObdScanner(car.getId()
                                                ,scannerId,scannerName);
                                    }
                                    scannerRepository.createScanner(scanner, new Repository.Callback() {
                                        @Override
                                        public void onSuccess(Object data) {

                                            //Process succeeded, notify eventbus
                                            EventType eventType
                                                    = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                            EventBus.getDefault().post(new CarDataChangedEvent(
                                                    eventType, eventSource));

                                            if(car.getShopId() == 0){
                                                callback.onCarAdded(car);
                                            }else {
                                                callback.onCarAddedWithBackendShop(car);
                                            }
                                        }
                                        @Override
                                        public void onError(RequestError error) {

                                            //Error adding scanner, but car was still added for the user
                                            EventType eventType
                                                    = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                            EventBus.getDefault().post(new CarDataChangedEvent(
                                                    eventType, eventSource));
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

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

    private String vin;
    private String scannerId;
    private String scannerName;
    private double baseMileage;


    public AddCarUseCaseImpl(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, Handler handler){
        this.carRepository = carRepository;
        this.scannerRepository = scannerRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String vin, double baseMileage, String scannerId
            ,String scannerName, String eventSource, Callback callback) {

        this.vin = vin;
        this.baseMileage = baseMileage;
        this.scannerId = scannerId;
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.scannerName = scannerName;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                    carRepository.getCarByVin(vin, new Repository.Callback<Car>() {
                        @Override
                        public void onSuccess(Car car) {
                            boolean carExists = car != null;
                            boolean hasUser = car != null && car.getUserId() != 0;
                            boolean hasScanner = car != null && car.getScannerId() != null
                                    && !car.getScannerId().isEmpty();

                            //If car exists and has user
                            if (carExists && hasUser){
                                callback.onCarAlreadyAdded(car);
                            }

                            //If car exists and does not have user but scanner not active/ active
                            else if (carExists && !hasUser && hasScanner){
                                scannerRepository.getScanner(scannerId, new Repository.Callback<ObdScanner>() {

                                    @Override
                                    public void onSuccess(ObdScanner obdScanner) {
                                        //Active, deactivate then add
                                        if (obdScanner.getStatus()){

                                            obdScanner.setStatus(false);
                                            scannerRepository.updateScanner(obdScanner, new Repository.Callback<Object>() {

                                                @Override
                                                public void onSuccess(Object response){
                                                    addCar(vin,baseMileage,user.getId(),scannerId
                                                            ,callback);
                                                }

                                                @Override
                                                public void onError(RequestError error){
                                                    callback.onError(error);
                                                }
                                            });
                                        }
                                        //Not active, add
                                        else{
                                            addCar(vin,baseMileage,user.getId(),scannerId,callback);
                                        }
                                    }

                                    @Override
                                    public void onError(RequestError error) {
                                        callback.onError(error);
                                    }
                                });
                            }

                            else if (carExists && !hasUser && !hasScanner){
                                addCar(vin,baseMileage,user.getId(),scannerId,callback);
                            }

                            //If car does not exist then add
                            else if (!carExists){
                                addCar(vin,baseMileage,user.getId(),scannerId,callback);
                            }

                            //Unknown case
                            else{
                                callback.onError(RequestError.getUnknownError());
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

    private void addCar(String vin, double baseMileage, int userId, String scannerId
            , Callback callback){
        carRepository.insert(vin, baseMileage, userId, scannerId
                , new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        userRepository.setUserCar(userId, car.getId(), new Repository.Callback<Object>() {
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
}

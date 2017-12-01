package com.pitstop.interactors.add;

import android.os.Handler;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Matt on 2017-07-27.
 */

public class AddCarUseCaseImpl implements AddCarUseCase {

    final private String TAG = getClass().getSimpleName();

    private CarRepository carRepository;
    private UserRepository userRepository;
    private ScannerRepository scannerRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;

    private EventSource eventSource;

    private String vin;
    private String scannerId;
    private String scannerName;
    private double baseMileage;


    public AddCarUseCaseImpl(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, Handler useCaseHandler, Handler mainHandler){
        this.carRepository = carRepository;
        this.scannerRepository = scannerRepository;
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(String vin, double baseMileage, String scannerId
            ,String scannerName, String eventSource, Callback callback) {

        Logger.getInstance().logI(TAG,"Use case execution started input: vin"+vin+", baseMileage: "+baseMileage+", scannerId: "
                +scannerId+", scannerName: "+scannerName+", eventSource: "+eventSource, DebugMessage.TYPE_USE_CASE);

        this.vin = vin;
        this.baseMileage = baseMileage;
        this.scannerId = scannerId;
        this.eventSource = new EventSourceImpl(eventSource);
        this.callback = callback;
        this.scannerName = scannerName;
        useCaseHandler.post(this);
    }

    private void onCarAddedWithBackendShop(Car car){
        Logger.getInstance().logI(TAG,"Use case finished: car added with backend shop, car= "+car
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarAddedWithBackendShop(car));
    }

    private void onCarAdded(Car car){
        Logger.getInstance().logI(TAG,"Use case finished: car added car= "+car
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarAdded(car));
    }

    private void onCarAlreadyAdded(Car car){
        Logger.getInstance().logI(TAG,"Use case finished: car already added!"
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onCarAlreadyAdded(car));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: "+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        Log.d(TAG,"run()");
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG,"getCurrentUser().onSuccess() user: "+user);
                carRepository.getCarByVin(vin, new Repository.Callback<Car>() {
                        @Override
                        public void onSuccess(Car car) {

                            boolean carExists = car != null;
                            boolean hasUser = car != null && car.getUserId() != 0;
                            boolean hasScanner = car != null && car.getScannerId() != null
                                    && !car.getScannerId().isEmpty();

                            Log.d(TAG,"getCarsByVin().onSuccess() carExists?"+carExists+", hasUser?"
                                    +hasUser+", hasScanner?"+hasScanner+", car: "+car);

                            //If statements are purposely not simplified here to make the code easy to read

                            //If car exists and has user
                            if (carExists && hasUser){
                                Log.d(TAG,"carExists && hasUser, calling callback.onCarAlreadyAdded()");
                                AddCarUseCaseImpl.this.onCarAlreadyAdded(car);
                            }

                            //If car exists and does not have user but scanner not active/ active
                            else if (carExists && !hasUser && hasScanner){
                                Log.d(TAG,"carExists && !hasUser && hasScanner, getting scanner");
                                scannerRepository.getScanner(scannerId, new Repository.Callback<ObdScanner>() {

                                    @Override
                                    public void onSuccess(ObdScanner obdScanner) {
                                        if (obdScanner == null){
                                            AddCarUseCaseImpl.this.onError(
                                                    RequestError.getUnknownError());
                                            return;
                                        }
                                        Log.d(TAG,"getScannerId.onSuccess() obdScanner.id: "
                                                +obdScanner.getScannerId()
                                                +", active?"+obdScanner.getStatus());

                                        //Active, deactivate then adds
                                        if (obdScanner.getStatus()){

                                            obdScanner.setStatus(false);
                                            scannerRepository.updateScanner(obdScanner, new Repository.Callback<Object>() {

                                                @Override
                                                public void onSuccess(Object response){
                                                    Log.d(TAG,"updateScanner().onSuccess() response:"
                                                            +response);
                                                    addCar(vin,baseMileage,user.getId(),scannerId
                                                            ,callback);
                                                }

                                                @Override
                                                public void onError(RequestError error){
                                                    Log.d(TAG,"updateScanner().onError() error: "
                                                            +error.getMessage());
                                                    AddCarUseCaseImpl.this.onError(error);
                                                }
                                            });
                                        }
                                        //Not active, add
                                        else{
                                            Log.d(TAG,"scanner not active, adding car");
                                            addCar(vin,baseMileage,user.getId(),scannerId,callback);
                                        }
                                    }

                                    @Override
                                    public void onError(RequestError error) {
                                        Log.d(TAG,"getScannerId().onError() error: "
                                                +error.getMessage());

                                        AddCarUseCaseImpl.this.onError(error);
                                    }
                                });
                            }

                            else if (carExists && !hasUser && !hasScanner){
                                Log.d(TAG,"carExists && !hasUser && !hasScanner, adding car");
                                addCar(vin,baseMileage,user.getId(),scannerId,callback);
                            }

                            //If car does not exist then add
                            else if (!carExists){
                                Log.d(TAG,"!carExists, adding car");
                                addCar(vin,baseMileage,user.getId(),scannerId,callback);
                            }

                            //Unknown case that is never reached
                            else{
                                AddCarUseCaseImpl.this.onError(RequestError.getUnknownError());
                            }


                        }

                        @Override
                        public void onError(RequestError error) {
                            AddCarUseCaseImpl.this.onError(error);
                        }
                    });

            }
            @Override
            public void onError(RequestError error) {
                AddCarUseCaseImpl.this.onError(error);
            }
        });
    }

    private void addCar(String vin, double baseMileage, int userId, String scannerId
            , Callback callback){
        Log.d(TAG,"addCar() vin: "+vin+", baseMileage: "+baseMileage+", userId: "+userId+", scannerId: "+scannerId);
        carRepository.insert(vin, baseMileage, userId, scannerId
                , new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        Log.d(TAG,"insert.onSuccess() car: "+car);

                        userRepository.setUserCar(userId, car.getId(), new Repository.Callback<Object>() {
                            @Override
                            public void onSuccess(Object data) {
                                Log.d(TAG,"setUsercar.onSuccess() response: "+data);

                                //Process succeeded, notify eventbus
                                EventType eventType
                                        = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                EventBus.getDefault().post(new CarDataChangedEvent(
                                        eventType, eventSource));

                                //Car has shop if id is 0, 1 on staging, or 19 on production
                                boolean carHasShop = car.getShopId() != 0 && (car.getShopId() != 1
                                        && !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE))
                                        || (car.getShopId() != 19
                                        && BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE));

                                if (!carHasShop) {
                                    AddCarUseCaseImpl.this.onCarAdded(car);
                                }
                                else{
                                    AddCarUseCaseImpl.this.onCarAddedWithBackendShop(car);
                                }
                            }

                            @Override
                            public void onError(RequestError error) {
                                Log.d(TAG,"setUserCar.onError() error: "+error.getMessage());
                                AddCarUseCaseImpl.this.onError(error);
                            }
                        });
                    }
                    @Override
                    public void onError(RequestError error) {
                        AddCarUseCaseImpl.this.onError(error);
                    }});
    }
}

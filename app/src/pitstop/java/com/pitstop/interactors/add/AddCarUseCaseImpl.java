package com.pitstop.interactors.add;

import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;

import com.google.gson.JsonObject;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.application.GlobalVariables;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.SmoochUtil;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

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
                addCar(vin, baseMileage, user, scannerId, callback);
            }
            @Override
            public void onError(RequestError error) {
                AddCarUseCaseImpl.this.onError(error);
            }
        });
    }

    private void addCar(String vin, double baseMileage, User user, String scannerId
            , Callback callback){
        Log.d(TAG,"addCar()");
        int userId = user.getId();
        carRepository.insert(vin, baseMileage, userId, scannerId
                , new Repository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {
                        Log.d(TAG, "insert.onSuccess() car: " + car);

                        SmoochUtil.Companion.sendUserAddedCarSmoochMessage(user, car);
                        SmoochUtil.Companion.setSmoochProperties(car);

                        if (car.getShop() == null) {
                            AddCarUseCaseImpl.this.onCarAdded(car);
                        } else {
                            AddCarUseCaseImpl.this.onCarAddedWithBackendShop(car);
                        }
                    }
                    @Override
                    public void onError(RequestError error) {
                        try {
                            JSONObject errorJson = new JSONObject(error.getError());
                            if (errorJson.has("message")) {
                                error.setMessage(errorJson.getString("message"));
                                AddCarUseCaseImpl.this.onError(error);
                                return;
                            }
                        } catch (JSONException e) { }
                        AddCarUseCaseImpl.this.onError(error);
                    }});
    }
}

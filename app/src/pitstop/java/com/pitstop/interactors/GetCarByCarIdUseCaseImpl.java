package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Matthew on 2017-06-20.
 */

public class GetCarByCarIdUseCaseImpl implements GetCarByCarIdUseCase {
    private CarRepository carRepository;
    private NetworkHelper networkHelper;
    private UserRepository userRepository;
    private Callback callback;
    private Handler handler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, NetworkHelper networkHelper, UserRepository userRepository, Handler handler) {
        this.networkHelper = networkHelper;
        this.handler = handler;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void execute(int carId,Callback callback) {
        this.callback = callback;
        this.carId = carId;
        handler.post(this);
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                carRepository.get(carId,user.getId(), new CarRepository.CarGetCallback() {
                    @Override
                    public void onCarGot(Car car) {
                        callback.onCarGot(car);
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


        /*carRepository.get(carId, new CarRepository.CarGetCallback() {
            @Override
            public void onCarGot(Car car) {
                userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
                    @Override
                    public void onGotUser(User user) {
                        networkHelper.getUserSettingsById(user.getId(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(response != null){
                                    try{
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                                        for(int i = 0 ; i < customShops.length() ; i++){
                                            JSONObject shop = customShops.getJSONObject(i);
                                            if(car.getDealership().getId() == shop.getInt("id")){
                                                Dealership dealership = Dealership.jsonToDealershipObject(shop.toString());
                                                car.setDealership(dealership);
                                            }
                                        }
                                        callback.onCarGot(car);
                                    }catch (JSONException e){
                                        callback.onError();
                                        e.printStackTrace();
                                    }
                                }else{
                                    callback.onError();
                                }
                            }
                        });
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
        });*/
    }
}

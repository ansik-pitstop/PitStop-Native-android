package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Matt on 2017-06-13.
 */

public class GetCarsByUserIdUseCaseImpl implements GetCarsByUserIdUseCase {
    private Handler handler;
    private CarRepository carRepository;
    private NetworkHelper networkHelper;
    private UserRepository userRepository;

    private GetCarsByUserIdUseCase.Callback callback;


    public GetCarsByUserIdUseCaseImpl(UserRepository userRepository, NetworkHelper networkHelper,CarRepository carRepository, Handler handler) {
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
        this.carRepository = carRepository;

    }

    @Override
    public void execute(GetCarsByUserIdUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
       /* userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
               carRepository.getCarsByUserId(user.getId(), new CarRepository.CarsGetCallback() {
                   @Override
                   public void onCarsGot(List<Car> cars) {
                       networkHelper.getUserSettingsById(user.getId(), new RequestCallback() {
                           @Override
                           public void done(String response, RequestError requestError) {
                               if(response != null){
                                   try{
                                       JSONObject responseJson = new JSONObject(response);
                                       JSONObject userJson = responseJson.getJSONObject("user");
                                       int mainCarId = userJson.getInt("mainCar");
                                       JSONArray customShops = userJson.getJSONArray("customShops");
                                       for(Car c:cars){
                                           if(c.getId() == mainCarId) {
                                               c.setCurrentCar(true);
                                           }
                                           for( int i = 0 ; i < customShops.length() ; i++){
                                               JSONObject shop = customShops.getJSONObject(i);
                                               if(c.getDealership().getId() == shop.getInt("id")){
                                                   c.setDealership(Dealership.jsonToDealershipObject(shop.toString()));
                                               }
                                           }
                                       }
                                       callback.onCarsRetrieved(cars);
                                   }catch(JSONException e){
                                       e.printStackTrace();
                                       callback.onError();
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



       userRepository.getCurrentUser(new UserRepository.UserGetCallback(){
            @Override
            public void onGotUser(User user) {
               carRepository.getCarsByUserId(user.getId(),new CarRepository.CarsGetCallback() {
                    @Override
                    public void onCarsGot(List<Car> cars) {
                        callback.onCarsRetrieved(cars);
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

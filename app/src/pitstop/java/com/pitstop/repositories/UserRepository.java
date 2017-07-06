package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User repository, use this class to modify, retrieve, and delete user data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

public class UserRepository {

    private static UserRepository INSTANCE;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;

    public interface UserSetCarCallback {
        void onSetCar();
        void onError();
    }

    public interface UserFirstCarAddedSetCallback {
        void onFirstCarAddedSet();
        void onError();
    }

    public interface CheckFirstCarAddedCallback {
        void onFirstCarAddedChecked(boolean added);
        void onError();
    }

    public interface UserGetCallback {
        void onGotUser(User user);
        void onError();
    }

    public interface UserUpdateCallback {
        void onUpdatedUser();
        void onError();
    }

    public interface UserInsertCallback {
        void onInsertedUser();
        void onError();
    }

    public interface UserGetCarCallback {
        void onGotCar(Car car);
        void onNoCarSet();
        void onError();
    }

    public static synchronized UserRepository getInstance(UserAdapter userAdapter
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new UserRepository(userAdapter, networkHelper);
        }
        return INSTANCE;
    }

    public UserRepository(UserAdapter userAdapter, NetworkHelper networkHelper){
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    public boolean insert(User model, UserInsertCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName()
                ,model.getPhone(),getInsertUserRequestCallback(callback));
        return true;
    }

    private RequestCallback getInsertUserRequestCallback(UserInsertCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onInsertedUser();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public boolean update(User model, UserUpdateCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName()
                ,model.getPhone(),getUserUpdateRequestCallback(callback));
        return true;
    }

    private RequestCallback getUserUpdateRequestCallback(UserUpdateCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onUpdatedUser();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public void getCurrentUser(UserGetCallback callback){
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        networkHelper.getUser(userAdapter.getUser()
                .getId(),getUserGetRequestCallback(callback));
    }

    public void get(int id, UserGetCallback callback) {
        networkHelper.getUser(id,getUserGetRequestCallback(callback));
    }

    private RequestCallback getUserGetRequestCallback(UserGetCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onGotUser(User.jsonToUserObject(response));
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public void setCurrentUser(User user){
        userAdapter.storeUserData(user);
    }

    public void removeAllUsers(){
        userAdapter.deleteAllUsers();
    }

    public void getUserCar(UserGetCarCallback callback){
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        if (userAdapter.getUser() == null){
            callback.onError();
            return;
        }
        networkHelper.getMainCar(userAdapter.getUser().getId(),getUserGetCarRequestCallback(callback));
    }

    private RequestCallback getUserGetCarRequestCallback(UserGetCarCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null && response != null){
                        Car car = Car.createCar(response);
                        networkHelper.getUserSettingsById(userAdapter.getUser().getId(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(response != null){
                                    try{
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                                        for(int i = 0 ; i < customShops.length() ; i++){
                                            JSONObject shop = customShops.getJSONObject(i);
                                            if(car.getDealership() != null){
                                                if(car.getDealership().getId() == shop.getInt("id")){
                                                    Dealership dealership = Dealership.jsonToDealershipObject(shop.toString());
                                                    dealership.setCustom(true);
                                                    car.setDealership(dealership);
                                                }
                                            }else{
                                                Dealership noDealer = new Dealership();
                                                noDealer.setName("No Dealership");
                                                noDealer.setId(19);
                                                noDealer.setEmail("info@getpitstop.io");
                                                noDealer.setCustom(true);
                                                car.setDealership(noDealer);
                                            }
                                        }
                                        callback.onGotCar(car);
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
                    else if (requestError == null && response == null){
                        callback.onNoCarSet();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){
                    callback.onError();
                }
            }
        };

        return requestCallback;
    }

    public void setUserCar(int userId, int carId, UserSetCarCallback callback){
        networkHelper.setMainCar(userId,carId,getUserSetCarRequestCallback(callback));
    }

    private RequestCallback getUserSetCarRequestCallback(UserSetCarCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSetCar();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){

                }
            }
        };

        return requestCallback;
    }

    public void setFirstCarAdded(final boolean added
            , final UserFirstCarAddedSetCallback callback){

        final int userId = userAdapter.getUser().getId();

        networkHelper.getUserSettingsById(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        //Get settings and add boolean
                        JSONObject settings = new JSONObject(response).getJSONObject("user");
                        settings.put("isFirstCarAdded", added);

                        JSONObject putSettings = new JSONObject();
                        putSettings.put("settings",settings);

                        RequestCallback requestCallback = getSetFirstCarAddedCallback(callback);

                        networkHelper.put("user/" + userId + "/settings", requestCallback, putSettings);
                    }
                    catch(JSONException e){}

                }
                else{
                    callback.onError();
                }
            }
        });
    }

    private RequestCallback getSetFirstCarAddedCallback(UserFirstCarAddedSetCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onFirstCarAddedSet();
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JsonIOException e){
                    callback.onError();
                }
            }
        };

        return requestCallback;
    }

    public void checkFirstCarAdded(final CheckFirstCarAddedCallback callback){

        networkHelper.getUserSettingsById(userAdapter.getUser().getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject options = new JSONObject(response).getJSONObject("user");
                        boolean added;
                        if (options.has("isFirstCarAdded")){
                            //New users will have this property
                            added = options.getBoolean("isFirstCarAdded");
                        }else{
                            //Users that have registered prior to this patch will not send greeting messages
                            added = true;
                        }

                        callback.onFirstCarAddedChecked(added);
                    }
                    catch(JSONException e){
                        callback.onError();
                    }

                }
                else{
                    callback.onError();
                }
            }
        });
    }

}

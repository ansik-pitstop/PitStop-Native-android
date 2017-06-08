package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

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

    public interface UserSetSmoochMessageSentCallback{
        void onSmoochMessageSentSet();
        void onError();
    }

    public interface IsSmoochMessageSentCallback{
        void onIsSmoochSentRetrieved(boolean sent);
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
        networkHelper.getMainCar(userAdapter.getUser().getId(),getUserGetCarRequestCallback(callback));
    }

    private RequestCallback getUserGetCarRequestCallback(UserGetCarCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onGotCar(Car.createCar(response));
                    }
                    else{
                        callback.onError();
                    }
                }
                catch(JSONException e){

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

    public void setUserSmoochMessageSent(final boolean sent,final int userId
            , final UserSetSmoochMessageSentCallback callback){

        networkHelper.getUserSettingsById(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject options = new JSONObject(response);
                        options.put("initialSmoochMessageSentOnce",sent);
                        RequestCallback requestCallback = getSetUserSentSmoochMessageCallback(callback);
                        networkHelper.put("user/" + userId + "/settings", requestCallback, options);
                    }
                    catch(JSONException e){}

                }
                else{
                    callback.onError();
                }
            }
        });
    }

    private RequestCallback getSetUserSentSmoochMessageCallback(UserSetSmoochMessageSentCallback callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSmoochMessageSentSet();
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

    public void isSmoochMessageSent(final int userId
            , final IsSmoochMessageSentCallback callback){

        networkHelper.getUserSettingsById(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null){
                    try{
                        JSONObject options = new JSONObject(response);
                        boolean sent = options.getBoolean("initialSmoochMessageSentOnce");
                        callback.onIsSmoochSentRetrieved(sent);
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

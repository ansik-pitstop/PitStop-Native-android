package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

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

    public User get(int id, UserGetCallback callback) {
        networkHelper.getUser(id,getUserGetRequestCallback(callback));
        return userAdapter.getUser();
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

}

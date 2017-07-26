package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.Settings;
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

public class UserRepository implements Repository{

    private final String END_POINT_SETTINGS = "settings/?userId=";
    private final String END_POINT_USER = "user/";

    private static UserRepository INSTANCE;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;

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
        return;
    }

    public void insert(User model, Callback<Object> callback) {
        if (!networkHelper.isConnected()){

            callback.onError(new RequestError());
            return;
        }

        userAdapter.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getInsertUserRequestCallback(callback));
    }

    private RequestCallback getInsertUserRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(null);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void update(User model, Callback<Object> callback) {
        userAdapter.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getUserUpdateRequestCallback(callback));
    }

    private RequestCallback getUserUpdateRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(null);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void getCurrentUser(Callback<User> callback){
        if (userAdapter.getUser() == null){
            callback.onError(RequestError.getUnknownError());
            return;
        }
        networkHelper.get(END_POINT_USER+userAdapter.getUser().getId()
                ,getUserGetRequestCallback(callback));
    }

    private RequestCallback getUserGetRequestCallback(Callback<User> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(User.jsonToUserObject(response));
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void setUserCar(int userId, int carId, Callback<Object> callback){

        getUserSettings(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null) {
                    try {
                        JSONObject options = new JSONObject(response).getJSONObject("user");
                        options.put("mainCar",carId);

                        JSONObject putOptions = new JSONObject();
                        putOptions.put("settings",options);

                        networkHelper.put("user/" + userId + "/settings", getUserSetCarRequestCallback(callback), putOptions);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    callback.onError(requestError);
                }
            }
        });
    }

    private RequestCallback getUserSetCarRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(response);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void setFirstCarAdded(final boolean added
            , final Callback<Object> callback){

        final int userId = userAdapter.getUser().getId();

        getUserSettings(userId, new RequestCallback() {
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
                    catch(JSONException e){
                        e.printStackTrace();
                        callback.onError(requestError);
                    }

                }
                else{
                    callback.onError(requestError);
                }
            }
        });
    }

    private RequestCallback getSetFirstCarAddedCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        callback.onSuccess(response);
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(requestError);
                }
            }
        };

        return requestCallback;
    }

    public void getCurrentUserSettings(Callback<Settings> callback){

        final int userId = userAdapter.getUser().getId();

        getUserSettings(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null){
                    callback.onError(requestError);
                    return;
                }
                try{
                    JSONObject settings = new JSONObject(response);
                    int carId = -1;
                    boolean firstCarAdded = true; //if not present, default is true

                    if (settings.getJSONObject("user").has("isFirstCarAdded")){
                        firstCarAdded = settings.getJSONObject("user").getBoolean("isFirstCarAdded");
                    }
                    if (settings.getJSONObject("user").has("mainCar")){
                        carId = settings.getJSONObject("user").getInt("mainCar");
                    }

                    if (carId == -1){
                        callback.onSuccess(new Settings(userId,firstCarAdded));
                    }
                    else{
                        callback.onSuccess(new Settings(userId,carId,firstCarAdded));
                    }

                }
                catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void getUserSettings(int userId, RequestCallback callback){
        networkHelper.get(END_POINT_SETTINGS+userId,callback);
    }

    private void updateUser(int userId, String firstName, String lastName, String phoneNumber, RequestCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("userId", userId);
            json.put("firstName", firstName);
            json.put("lastName", lastName);
            json.put("phone", phoneNumber);
            networkHelper.put(END_POINT_USER, callback, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

package com.pitstop.repositories;

import com.google.gson.JsonIOException;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
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

    public void insert(User model, UserInsertCallback callback) {
        userAdapter.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getInsertUserRequestCallback(callback));
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

    public void update(User model, UserUpdateCallback callback) {
        userAdapter.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getUserUpdateRequestCallback(callback));
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
        if(!networkHelper.isConnected() || userAdapter.getUser() == null){
            callback.onError();
        }
        networkHelper.get(END_POINT_USER+userAdapter.getUser().getId()
                ,getUserGetRequestCallback(callback));
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

    public void setUserCar(int userId, int carId, UserSetCarCallback callback){
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
                    callback.onError();
                }
            }
        });
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

    public void getCurrentUserSettings(Callback<Settings> callback){

        final int userId = userAdapter.getUser().getId();

        getUserSettings(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null){
                    callback.onError(requestError.getStatusCode());
                    return;
                }
                try{
                    JSONObject settings = new JSONObject(response);
                    int carId = -1;
                    boolean firstCarAdded = false;

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

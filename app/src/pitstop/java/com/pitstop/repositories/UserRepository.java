package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.JsonIOException;
import com.pitstop.database.LocalUserStorage;
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

    private final String TAG = getClass().getSimpleName();

    private final String END_POINT_SETTINGS = "settings/?userId=";
    private final String END_POINT_USER = "user/";

    private static UserRepository INSTANCE;
    private LocalUserStorage localUserStorage;
    private NetworkHelper networkHelper;

    private Settings cachedSettings = null;

    public static synchronized UserRepository getInstance(LocalUserStorage localUserStorage
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new UserRepository(localUserStorage, networkHelper);
        }
        return INSTANCE;
    }

    public UserRepository(LocalUserStorage localUserStorage, NetworkHelper networkHelper){
        this.localUserStorage = localUserStorage;
        this.networkHelper = networkHelper;
        return;
    }

    public void insert(User model, Callback<Object> callback) {
        Log.d(TAG,"insert() model: "+model);
        if (!networkHelper.isConnected()){

            callback.onError(new RequestError());
            return;
        }

        localUserStorage.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getInsertUserRequestCallback(callback));
    }

    private RequestCallback getInsertUserRequestCallback(Callback<Object> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
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
                callback.onError(RequestError.getUnknownError());
            }
        };

        return requestCallback;
    }

    public void update(User model, Callback<Object> callback) {
        Log.d(TAG,"update() user: "+model);
        localUserStorage.storeUserData(model);
        updateUser(model.getId(),model.getFirstName(),model.getLastName()
            ,model.getPhone(),getUserUpdateRequestCallback(callback,model));
    }

    private RequestCallback getUserUpdateRequestCallback(Callback<Object> callback
            , User user){
        //Create corresponding request callback
        RequestCallback requestCallback = (response, requestError) -> {
            try {
                if (requestError == null){
                    User newUser = localUserStorage.getUser();
                    user.setFirstName(user.getFirstName());
                    user.setLastName(user.getLastName());
                    user.setPhone(user.getPhone());
                    localUserStorage.storeUserData(newUser);
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
        };

        return requestCallback;
    }

    public void getCurrentUser(Callback<User> callback){
        Log.d(TAG,"getCurrentUser()");
        if (localUserStorage.getUser() == null){
            callback.onError(RequestError.getUnknownError());
            return;
        }else {
            callback.onSuccess(localUserStorage.getUser());
            return;
        }
//        }else{
//            Log.d("userID", Integer.toString(localUserStorage.getUser().getId()));
//            networkHelper.get(END_POINT_USER+ localUserStorage.getUser().getId()
//                    ,getUserGetRequestCallback(callback));
//        }
    }

    public void getRemoteCurrentUser(Callback<User> callback){
        Log.d(TAG,"getRemoteCurrentUser()");
        networkHelper.get(END_POINT_USER+ localUserStorage.getUser().getId()
                    ,getUserGetRequestCallback(callback));
    }

    private RequestCallback getUserGetRequestCallback(Callback<User> callback){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        Log.d("userresponse", response);
                        callback.onSuccess(User.jsonToUserObject(response));
                    }
                    else{
                        callback.onError(requestError);
                    }
                }
                catch(JsonIOException e){
                    e.printStackTrace();
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };

        return requestCallback;
    }

    public void setUserCar(int userId, int carId, Callback<Object> callback){
        Log.d(TAG,"setUserCar() userId: "+userId+", carId: "+carId+", cachedSettings: "+cachedSettings);
        getUserSettings(userId, (response, requestError) -> {
            if (requestError == null) {
                try {
                    JSONObject options = new JSONObject(response).getJSONObject("user");
                    options.put("mainCar",carId);
                    JSONObject putOptions = new JSONObject();
                    putOptions.put("settings",options);

                    networkHelper.put("user/" + userId + "/settings", getUserSetCarRequestCallback(callback, carId), putOptions);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                callback.onError(requestError);
            }
        });
    }

    private RequestCallback getUserSetCarRequestCallback(Callback<Object> callback, int carId){
        //Create corresponding request callback
        return (response, requestError) -> {
            Log.d(TAG,"set user settings response: "+response+", requestError: "+requestError);
            try {
                if (requestError == null){
                    if (cachedSettings != null)
                        cachedSettings.setCarId(carId);
                    Log.d(TAG,"cached settings after setting: "+cachedSettings);
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
        };
    }

    public void setFirstCarAdded(final boolean added
            , final Callback<Object> callback){

        Log.d(TAG,"setFirstCarAdded() added: "+added);
        final int userId = localUserStorage.getUser().getId();

        getUserSettings(userId, (response, requestError) -> {
            if (requestError == null){
                try{
                    //Get settings and add boolean
                    JSONObject settings = new JSONObject(response).getJSONObject("user");
                    settings.put("isFirstCarAdded", added);

                    JSONObject putSettings = new JSONObject();
                    putSettings.put("settings",settings);

                    RequestCallback requestCallback = getSetFirstCarAddedCallback(callback,added);

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
        });
    }




    private RequestCallback getSetFirstCarAddedCallback(Callback<Object> callback, boolean added){
        //Create corresponding request callback
        return (response, requestError) -> {
            try {
                if (requestError == null){
                    if (cachedSettings != null)
                        cachedSettings.setFirstCarAdded(added);
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
        };
    }



    public void getCurrentUserSettings(Callback<Settings> callback){
        Log.d(TAG,"getCurrentUserSettings() cached settings: "+cachedSettings);

        if(localUserStorage.getUser() == null){
            callback.onError(RequestError.getUnknownError());
            return;
        }
        else if (cachedSettings != null){
            callback.onSuccess(cachedSettings);
            return;
        }

        final int userId = localUserStorage.getUser().getId();

        getUserSettings(userId, (response, requestError) -> {
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

                if (carId == -1 && cachedSettings == null){
                    cachedSettings = new Settings(userId,firstCarAdded);
                    callback.onSuccess(cachedSettings);
                }
                else if (cachedSettings == null){
                    cachedSettings = new Settings(userId,carId,firstCarAdded);
                    callback.onSuccess(cachedSettings);
                }else{
                    callback.onSuccess(cachedSettings);
                }

            }
            catch(JSONException e){
                e.printStackTrace();
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

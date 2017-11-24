package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Matthew on 2017-06-21.
 */

public class ShopRepository implements Repository{

    private final String TAG = getClass().getSimpleName();

    private final String END_POINT_SHOP_PITSTOP = "shop?shopType=partner";
    private final String END_POINT_SHOP_ALL = "shop?shopType=all";
    private final String END_POINT_SHOP = "shop";

    private static ShopRepository INSTANCE;
    private LocalShopStorage localShopStorage;
    private NetworkHelper networkHelper;
    private boolean removeLocalShop;

    public static synchronized ShopRepository getInstance(LocalShopStorage localShopStorage, NetworkHelper networkHelper){
        if(INSTANCE == null){
            INSTANCE = new ShopRepository(localShopStorage, networkHelper);
        }
        return INSTANCE;
    }

    public ShopRepository(LocalShopStorage localShopStorage, NetworkHelper networkHelper){
        this.localShopStorage = localShopStorage;
        this.networkHelper = networkHelper;
    }

    public boolean insertPitstopShop(Dealership dealership, Callback<Object> callback ){
        if(localShopStorage.getDealership(dealership.getId()) != null){
            return false;
        }
        localShopStorage.storeDealership(dealership);
        return true;
    }

    public void getAllShops(Callback<List<Dealership>> callback){

        List<Dealership> localDealerships = localShopStorage.getAllDealerships();
        if (localDealerships.size() > 0){
            callback.onSuccess(localDealerships);
            return;
        }

        networkHelper.get(END_POINT_SHOP_ALL, (response, requestError) -> {
            if(response != null){
                try{
                    List<Dealership> dealerships = Dealership.createDealershipList(response);
                    localShopStorage.removeAllDealerships();
                    localShopStorage.storeDealerships(dealerships);
                    callback.onSuccess(dealerships);
                }catch(JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onError(RequestError.getUnknownError());
                }
            }else{
                callback.onError(requestError);
            }

        });
    }

    public void getPitstopShops(Callback<List<Dealership>> callback){
        networkHelper.get(END_POINT_SHOP_PITSTOP, (response, requestError) -> {
            if(response != null){
                try{
                    List<Dealership> dealerships = Dealership.createDealershipList(response);
                    for (Dealership d: dealerships){
                        localShopStorage.removeById(d.getId());
                        localShopStorage.storeDealership(d);
                    }
                    callback.onSuccess(dealerships);
                }catch(JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onError(RequestError.getUnknownError());
                }
            }else{
                callback.onError(requestError);
            }

        });

        //Offline logic below, not being used as of n
        List<Dealership> dealerships = localShopStorage.getAllDealerships();
        Iterator<Dealership> iterator = dealerships.iterator();
        while(iterator.hasNext()){
            Dealership d = iterator.next();
            if(d.isCustom()){
                iterator.remove();
            }
        }
    }

    public void insert(Dealership dealership, int userId, Callback<Object> callback){
        removeLocalShop = false;

        JSONObject body = new JSONObject();
        try {
            body.put("name",dealership.getName());
            body.put("email",dealership.getEmail());
            body.put("phone",dealership.getPhone());
            body.put("address",dealership.getAddress());
            body.put("googlePlacesId","");// to be added
        }catch (JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);

        }
        networkHelper.post(END_POINT_SHOP
                ,getInsertShopRequestCallback(callback,userId,dealership),body);
    }

    private RequestCallback getInsertShopRequestCallback(Callback<Object> callback, int userId, Dealership dealership){
        //Create corresponding request callback
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                try {
                    if (requestError == null){
                        JSONObject shopResponse = new JSONObject(response);
                        dealership.setId(shopResponse.getInt("id"));
                        networkHelper.getUserSettingsById(userId, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                try{
                                    if(requestError == null){
                                        JSONObject responseJson = new JSONObject(response);
                                        JSONObject userJson = responseJson.getJSONObject("user");
                                        JSONArray customShops = new JSONArray();
                                        JSONObject userSettingsDealer = new JSONObject();
                                        userSettingsDealer.put("id",dealership.getId());
                                        userSettingsDealer.put("name",dealership.getName());
                                        userSettingsDealer.put("email",dealership.getEmail());
                                        userSettingsDealer.put("phone_number",dealership.getPhone());
                                        userSettingsDealer.put("address",dealership.getAddress());
                                        if(userJson.has("customShops")){
                                            JSONArray shopsToSend = new JSONArray();
                                            customShops = userJson.getJSONArray("customShops");
                                            for(int i = 0;i<customShops.length();i++){
                                                JSONObject j = customShops.getJSONObject(i);
                                                if(j.getInt("id") != userSettingsDealer.getInt("id")){
                                                    shopsToSend.put(j);
                                                }else{
                                                    removeLocalShop = true;
                                                }
                                            }
                                            shopsToSend.put(userSettingsDealer);
                                            userJson.remove("customShops");
                                            userJson.put("customShops",shopsToSend);
                                        }else{
                                            customShops.put(userSettingsDealer);
                                            userJson.put("customShops",customShops);
                                        }
                                        JSONObject userSettings = new JSONObject();
                                        userSettings.put("settings",userJson);
                                        networkHelper.put("user/" + userId + "/settings", new RequestCallback() {
                                            @Override
                                            public void done(String response, RequestError requestError) {
                                                if(response!=null){
                                                    if(removeLocalShop){
                                                        localShopStorage.removeById(dealership.getId());
                                                    }
                                                    localShopStorage.storeCustom(dealership);
                                                    callback.onSuccess(response);
                                                }else{
                                                    callback.onError(requestError);
                                                }
                                            }
                                        },userSettings);
                                    }else{
                                        callback.onError(requestError);
                                    }
                                }catch (JSONException e){
                                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                                    callback.onError(RequestError.getUnknownError());
                                }
                            }
                        });
                    }else{
                        callback.onError(requestError);
                    }
                }catch(JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onError(RequestError.getUnknownError());
                }
            }
        };

        return requestCallback;
    }

    public void update(Dealership dealership,int userId, Callback<Object> callback ){
        networkHelper.getUserSettingsById(userId,getUpdateShopRequestCallback(dealership, userId, callback));

    }

    private RequestCallback getUpdateShopRequestCallback(Dealership dealership,int userId, Callback<Object> callback){
        RequestCallback requestCallback = (response, requestError) -> {
            if(response != null){
                try{
                    JSONObject responseJson = new JSONObject(response);
                    JSONObject userJson = responseJson.getJSONObject("user");
                    JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                    JSONArray shopsToSend = new JSONArray();
                    JSONObject userSettingsDealer = new JSONObject();
                    userSettingsDealer.put("id",dealership.getId());
                    userSettingsDealer.put("name",dealership.getName());
                    userSettingsDealer.put("email",dealership.getEmail());
                    userSettingsDealer.put("phone_number",dealership.getPhone());
                    userSettingsDealer.put("address",dealership.getAddress());
                    for(int i = 0; i<customShops.length();i++){
                        JSONObject shop = customShops.getJSONObject(i);
                        if(shop.getInt("id") != dealership.getId()){
                            shopsToSend.put(shop);
                        }
                    }
                    shopsToSend.put(userSettingsDealer);
                    userJson.remove("customShops");
                    userJson.put("customShops",shopsToSend);
                    JSONObject userSettings = new JSONObject();
                    userSettings.put("settings",userJson);
                    networkHelper.put("user/" + userId + "/settings", new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if(response != null){
                                callback.onSuccess(response);
                                localShopStorage.removeById(dealership.getId());
                                localShopStorage.storeCustom(dealership);
                            }else{
                                callback.onError(requestError);
                            }
                        }
                    },userSettings);
                }catch(JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onError(RequestError.getUnknownError());
                }

            }else{
                callback.onError(requestError);
            }
        };
        return requestCallback;

    }


    public void delete(int shopId,int userId, Callback<Object> callback){
        networkHelper.getUserSettingsById(userId,getDeleteShopRequestCallback(callback,userId,shopId) );
    }

    private RequestCallback getDeleteShopRequestCallback(Callback<Object> callback, int userId, int shopId){
        RequestCallback requestCallback = (response, requestError) -> {
            if(response != null){
                try{
                    JSONObject responseJson = new JSONObject(response);
                    JSONObject userJson = responseJson.getJSONObject("user");
                    JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                    JSONArray shopsToSend = new JSONArray();
                    for(int i = 0; i <customShops.length();i++){
                        JSONObject shop = customShops.getJSONObject(i);
                        if(shop.getInt("id") != shopId){
                            shopsToSend.put(shop);
                        }
                    }
                    userJson.remove("customShops");
                    JSONObject userSettings = new JSONObject();
                    userJson.put("customShops",shopsToSend);
                    userSettings.put("settings",userJson);
                    networkHelper.put("user/" + userId + "/settings", new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            callback.onSuccess(response);
                        }
                    },userSettings);
                }catch(JSONException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    callback.onError(RequestError.getUnknownError());
                }
            }else{
                callback.onError(requestError);
            }
        };
        return requestCallback;
    }


    public void get(int dealerId, Callback<Dealership> callback){
        Log.d(TAG,"get() "+END_POINT_SHOP+"/"+dealerId);

        if (dealerId == 0 && (BuildConfig.DEBUG
                || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA))){
            dealerId = 1;
        }
        else if (dealerId == 0 && BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)){
            dealerId = 19;
        }
        Dealership localDealership = localShopStorage.getDealership(dealerId);
        if (localDealership != null){
            callback.onSuccess(localDealership);
            return;
        }
        else networkHelper.get(END_POINT_SHOP+"/"+dealerId,getGetShopRequestCallback(callback));

        //Offline logic below, not being used for now
        //return localShopAdapter.getDealership(dealerId);
    }

    private RequestCallback getGetShopRequestCallback(Callback<Dealership> callback){
        RequestCallback requestCallback = (response, requestError) -> {
            if(response != null){
                //try{
                    Log.d(TAG,"get shop response: "+response);
                    Dealership dealership = Dealership.jsonToDealershipObject(response);
                    if (dealership == null){
                        callback.onError(RequestError.getUnknownError());
                    }else{
                        callback.onSuccess(dealership);
                    }
                //}catch (JSONException e){
                   // e.printStackTrace();
                   // callback.onError(RequestError.getUnknownError());
                //}

            }else{
                callback.onError(requestError);
            }
        };
        return requestCallback;
    }

}

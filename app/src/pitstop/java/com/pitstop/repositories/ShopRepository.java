package com.pitstop.repositories;

import android.util.Log;

import com.pitstop.database.LocalShopStorage;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Matthew on 2017-06-21.
 */

public class ShopRepository implements Repository{

    private final String TAG = getClass().getSimpleName();

    private final String END_POINT_SHOP_PITSTOP = "shop?shopType=partner";
    private final String END_POINT_SHOP = "shop?shopType=all";

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

    public void getPitstopShops(Callback<List<Dealership>> callback){
        networkHelper.get(END_POINT_SHOP_PITSTOP, getGetShopsCallback(callback));

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

    private RequestCallback getGetShopsCallback(Callback<List<Dealership>> callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(response != null){
                    try{
                        List<Dealership> dealerships = Dealership.createDealershipList(response);
                        localShopStorage.storeDealerships(dealerships);
                        callback.onSuccess(dealerships);
                    }catch(JSONException e){
                        callback.onError(RequestError.getUnknownError());
                        e.printStackTrace();
                    }
                }else{
                    callback.onError(requestError);
                }

            }
        };
        return requestCallback;
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
            e.printStackTrace();
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
                                    callback.onError(RequestError.getUnknownError());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }else{
                        callback.onError(requestError);
                    }
                }catch(JSONException e){
                    callback.onError(RequestError.getUnknownError());
                    e.printStackTrace();
                }
            }
        };

        return requestCallback;
    }

    public void getShopsByUserId(int userId, Callback<List<Dealership>> callback){

        networkHelper.get(END_POINT_SHOP, getGetShopsCallback(callback));
        //Offline logic below, not being used for now
        List<Dealership> dealerships = localShopStorage.getAllDealerships();
        Iterator<Dealership> iterator = dealerships.iterator();
        while(iterator.hasNext()){
            Dealership d = iterator.next();
            if(!d.isCustom()){
                iterator.remove();
            }
        }
        //return dealerships;
    }

    private  RequestCallback getGetShopsRequestCallback(Callback<List<Dealership>> callback){
     RequestCallback requestCallback = new RequestCallback() {
         @Override
         public void done(String response, RequestError requestError) {
             if(response != null){
                 try{
                     JSONObject responseJson = new JSONObject(response);
                     if(!responseJson.getJSONObject("user").has("customShops")){
                         callback.onSuccess(new ArrayList<Dealership>());
                     }else{
                         JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                         List<Dealership> dealershipArray  = new ArrayList<>();
                         for(int i = 0; i<customShops.length();i++){
                             Dealership dealership = new Dealership();
                             JSONObject shop = customShops.getJSONObject(i);
                             dealership.setId(shop.getInt("id"));
                             dealership.setName(shop.getString("name"));
                             dealership.setAddress(shop.getString("address"));
                             dealership.setEmail(shop.getString("email"));
                             dealership.setPhoneNumber(shop.getString("phone_number"));
                             dealership.setCustom(true);
                             dealershipArray.add(dealership);
                             localShopStorage.removeById(dealership.getId());
                             localShopStorage.storeCustom(dealership);
                         }
                         callback.onSuccess(dealershipArray);
                     }
                 }catch (JSONException e){
                     callback.onError(RequestError.getUnknownError());
                     e.printStackTrace();
                 }
             }else{
                 callback.onError(requestError);
             }
         }
     };
     return requestCallback;
    }

    public void update(Dealership dealership,int userId, Callback<Object> callback ){
        networkHelper.getUserSettingsById(userId,getUpdateShopRequestCallback(dealership, userId, callback));

    }

    private RequestCallback getUpdateShopRequestCallback(Dealership dealership,int userId, Callback<Object> callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
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
                        callback.onError(RequestError.getUnknownError());
                        e.printStackTrace();
                    }

                }else{
                    callback.onError(requestError);
                }
            }
        };
        return requestCallback;

    }


    public void delete(int shopId,int userId, Callback<Object> callback){
        networkHelper.getUserSettingsById(userId,getDeleteShopRequestCallback(callback,userId,shopId) );
    }

    private RequestCallback getDeleteShopRequestCallback(Callback<Object> callback, int userId, int shopId){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
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
                        callback.onError(RequestError.getUnknownError());
                        e.printStackTrace();
                    }
                }else{
                    callback.onError(requestError);
                }
            }
        };
        return requestCallback;
    }


    public void get(int dealerId, int userId, Callback<Dealership> callback){

        networkHelper.get(END_POINT_SHOP+"?id="+dealerId,getGetShopRequestCallback(callback));

        //Offline logic below, not being used for now
        //return localShopAdapter.getDealership(dealerId);
    }

    private RequestCallback getGetShopRequestCallback(Callback<Dealership> callback){
        RequestCallback requestCallback = (response, requestError) -> {
            if(response != null){
                Log.d(TAG,"get shops response: "+response);
                Dealership dealership = Dealership.jsonToDealershipObject(response);
                callback.onSuccess(dealership);
            }else{
                callback.onError(requestError);
            }
        };
        return requestCallback;
    }

}

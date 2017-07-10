package com.pitstop.repositories;

import com.pitstop.database.LocalShopHelper;
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

public class ShopRepository {
    private static ShopRepository INSTANCE;
    private LocalShopHelper localShopHelper;
    private NetworkHelper networkHelper;
    private boolean removeLocalShop;

    public interface ShopInsertCallback{
        void onShopAdded();
        void onError();
    }

    public interface ShopUpdateCallback{
        void onShopUpdated();
        void onError();
    }

    public interface ShopGetCallback{
        void onShopGot(Dealership dealership);
        void onError();
    }

    public interface ShopsGetCallback{
        void onShopsGot(List<Dealership> dealerships);
        void onError();
    }
    public interface ShopDeleteCallback{
        void onShopDeleted();
        void onError();
    }

    public interface InsertPitstopShopCallback{
        void onShopAdded();
        void onError();
    }

    public interface GetPitstopShopsCallback{
        void onShopsGot(List<Dealership> dealershipList);
        void onError();
    }



    public static synchronized ShopRepository getInstance(LocalShopHelper localShopHelper, NetworkHelper networkHelper){
        if(INSTANCE == null){
            INSTANCE = new ShopRepository(localShopHelper, networkHelper);
        }
        return INSTANCE;
    }

    public ShopRepository(LocalShopHelper localShopHelper, NetworkHelper networkHelper){
        this.localShopHelper = localShopHelper;
        this.networkHelper = networkHelper;
    }

    public boolean insertPitstopShop(Dealership dealership, InsertPitstopShopCallback callback ){
        if(localShopHelper.getDealership(dealership.getId()) != null){
            return false;
        }
        localShopHelper.storeDealership(dealership);
        return true;
    }

    public List<Dealership> getPitstopShops(GetPitstopShopsCallback callback){
        networkHelper.getPitStopShops(getGetPitstopShopsREquestCallback(callback));
        List<Dealership> dealerships = localShopHelper.getAllDealerships();
        Iterator<Dealership> iterator = dealerships.iterator();
        while(iterator.hasNext()){
            Dealership d = iterator.next();
            if(d.isCustom()){
                iterator.remove();
            }
        }
        return dealerships;
    }

    private RequestCallback getGetPitstopShopsREquestCallback(GetPitstopShopsCallback callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(response != null){
                    try{
                        List<Dealership> dealerships = Dealership.createDealershipList(response);
                        localShopHelper.storeDealerships(dealerships);
                        callback.onShopsGot(dealerships);
                    }catch(JSONException e){
                        callback.onError();
                        e.printStackTrace();
                    }
                }else{
                    callback.onError();
                }

            }
        };
        return requestCallback;
    }

    public boolean insert(Dealership dealership, int userId, ShopInsertCallback callback){
        removeLocalShop = false;
        networkHelper.postShop(dealership,getInsertShopRequestCallback(callback,userId,dealership));
        return true;
    }

    private RequestCallback getInsertShopRequestCallback(ShopInsertCallback callback, int userId, Dealership dealership){
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
                                                        localShopHelper.removeById(dealership.getId());
                                                    }
                                                    localShopHelper.storeCustom(dealership);
                                                    callback.onShopAdded();
                                                }else{
                                                    callback.onError();
                                                }
                                            }
                                        },userSettings);
                                    }else{
                                        callback.onError();
                                    }
                                }catch (JSONException e){
                                    callback.onError();
                                    e.printStackTrace();
                                }
                            }
                        });
                    }else{
                        callback.onError();
                    }
                }catch(JSONException e){
                    callback.onError();
                    e.printStackTrace();
                }
            }
        };

        return requestCallback;
    }

    public List<Dealership> getShopsByUserId(int userId, ShopsGetCallback callback){
        networkHelper.getUserSettingsById(userId,getGetShopsRequestCallback(callback));
        List<Dealership> dealerships = localShopHelper.getAllDealerships();
        Iterator<Dealership> iterator = dealerships.iterator();
        while(iterator.hasNext()){
            Dealership d = iterator.next();
            if(!d.isCustom()){
                iterator.remove();
            }
        }
        return dealerships;
    }

    private  RequestCallback getGetShopsRequestCallback(ShopsGetCallback callback){
     RequestCallback requestCallback = new RequestCallback() {
         @Override
         public void done(String response, RequestError requestError) {
             if(response != null){
                 try{
                     JSONObject responseJson = new JSONObject(response);
                     if(!responseJson.getJSONObject("user").has("customShops")){
                         callback.onShopsGot(new ArrayList<Dealership>());
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
                             localShopHelper.removeById(dealership.getId());
                             localShopHelper.storeCustom(dealership);
                         }
                         callback.onShopsGot(dealershipArray);
                     }
                 }catch (JSONException e){
                     callback.onError();
                     e.printStackTrace();
                 }
             }else{
                 callback.onError();
             }
         }
     };
     return requestCallback;
    }

    public boolean update(Dealership dealership,int userId, ShopUpdateCallback callback ){
        if(localShopHelper.getDealership(dealership.getId()) == null){
            return false;
        }
        networkHelper.getUserSettingsById(userId,getUpdateShopRequestCallback(dealership, userId, callback));

        return true;
    }

    private RequestCallback getUpdateShopRequestCallback(Dealership dealership,int userId, ShopUpdateCallback callback){
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
                                    callback.onShopUpdated();
                                    localShopHelper.removeById(dealership.getId());
                                    localShopHelper.storeCustom(dealership);
                                }else{
                                    callback.onError();
                                }
                            }
                        },userSettings);
                    }catch(JSONException e){
                        callback.onError();
                        e.printStackTrace();
                    }

                }else{
                    callback.onError();
                }
            }
        };
        return requestCallback;

    }


    public boolean delete(int shopId,int userId, ShopDeleteCallback callback){
        localShopHelper.removeById(shopId);
        networkHelper.getUserSettingsById(userId,getDeleteShopRequestCallback(callback,userId,shopId) );
        return true;
    }

    private RequestCallback getDeleteShopRequestCallback(ShopDeleteCallback callback, int userId, int shopId){
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
                                callback.onShopDeleted();
                            }
                        },userSettings);
                    }catch(JSONException e){
                        callback.onError();
                        e.printStackTrace();
                    }
                }else{
                    callback.onError();
                }
            }
        };
        return requestCallback;
    }


    public Dealership get(int dealerId, int userId, ShopGetCallback callback){
        networkHelper.getUserSettingsById(userId,getGetShopRequestCallback(callback, dealerId));
        return localShopHelper.getDealership(dealerId);
    }

    private RequestCallback getGetShopRequestCallback(ShopGetCallback callback, int dealerId){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(response != null){
                    try{
                        JSONObject responseJson = new JSONObject(response);
                        JSONArray customShops = responseJson.getJSONObject("user").getJSONArray("customShops");
                        for(int i = 0; i<customShops.length();i++){
                            JSONObject shop = customShops.getJSONObject(i);
                            if(shop.getInt("id") == dealerId){
                                Dealership dealership = new Dealership();
                                dealership.setId(shop.getInt("id"));
                                dealership.setName(shop.getString("name"));
                                dealership.setAddress(shop.getString("address"));
                                dealership.setEmail(shop.getString("email"));
                                dealership.setPhoneNumber(shop.getString("phone_number"));
                                dealership.setCustom(true);
                                localShopHelper.removeById(dealership.getId());
                                localShopHelper.storeCustom(dealership);
                                callback.onShopGot(dealership);
                                break;
                            }
                        }
                    }catch(JSONException e){
                        callback.onError();
                        e.printStackTrace();
                    }
                }else{
                    callback.onError();
                }
            }
        };
        return requestCallback;
    }

}
